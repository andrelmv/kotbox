package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.utils.isDataClass
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry

/**
 * Analyzes a Kotlin data-class hierarchy via PSI and produces a [ProtoMessage]
 * tree that [ProtoRenderer] can later serialize to `.proto` text.
 *
 * **Deduplication**: [processed] caches fully analyzed classes by FQN — prevents duplicate message blocks.
 * Uses FQN as the key to avoid false collisions between same-named classes in different packages.
 *
 * Further enhancement: migrate to K2 Analysis API for robust type resolution https://github.com/Kotlin/analysis-api
 */
internal class ProtoAnalyzer(
    private val project: Project,
    private val scope: GlobalSearchScope,
) {
    // Guards against circular references in nullable data class fields (e.g. A(val b: B?) and B(val a: A?)).
    // Extremely rare in practice but would cause a StackOverflowError without this guard.
    private val processed = mutableMapOf<String, ProtoMessage>()

    fun analyze(rootClass: KtClass): ProtoMessage {
        require(rootClass.isDataClass()) { "'${rootClass.name}' is not a data class" }
        return processClass(rootClass)
    }

    private fun processClass(ktClass: KtClass): ProtoMessage {
        val className = ktClass.name!!

        val qualifiedName = ktClass.fqName?.asString() ?: className
        processed[qualifiedName]?.let { return it }

        val fields =
            ktClass.primaryConstructorParameters
                .mapNotNull { param ->
                    val paramName = param.name ?: return@mapNotNull null
                    val typeText = param.typeReference?.text ?: return@mapNotNull null
                    paramName to typeText
                }.mapIndexed { index, (paramName, typeText) ->
                    resolveField(
                        name = paramName,
                        typeText = typeText,
                        number = index + 1,
                    )
                }

        return ProtoMessage(
            name = className,
            fields = fields,
        ).also {
            processed[qualifiedName] = it
        }
    }

    private fun resolveField(
        name: String,
        typeText: String,
        number: Int,
    ): ProtoField {
        val isNullable = typeText.endsWith('?')
        return when (val resolved = ProtoTypeMapper.resolve(typeText)) {
            is MappedProtoType.ScalarType ->
                ProtoField(
                    name = name,
                    number = number,
                    fieldType =
                        ProtoFieldType.Scalar(
                            protoType = resolved.protoType,
                            modifier = if (resolved.isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE,
                        ),
                )

            is MappedProtoType.CollectionType -> {
                val nested =
                    resolved.elementProto
                        .takeIf { resolved.isCustomType }
                        ?.let(::findDataClass)
                        ?.let(::processClass)

                ProtoField(
                    name = name,
                    number = number,
                    fieldType = ProtoFieldType.Repeated(resolved.elementProto),
                    nestedMessage = nested,
                )
            }

            is MappedProtoType.MapType -> {
                val nested =
                    resolved.valueProto
                        .takeIf { resolved.isCustomValue }
                        ?.let(::findDataClass)
                        ?.let(this::processClass)

                ProtoField(
                    name = name,
                    number = number,
                    fieldType = ProtoFieldType.Map(resolved.keyProto, resolved.valueProto),
                    nestedMessage = nested,
                )
            }

            null -> {
                handleUnmappedType(
                    name = name,
                    number = number,
                    baseType = typeText.trimEnd('?').trim(),
                    isNullable = isNullable,
                )
            }
        }
    }

    /**
     * Handles custom types that were not possible to map to a Protobuf type
     */
    private fun handleUnmappedType(
        name: String,
        number: Int,
        baseType: String,
        isNullable: Boolean,
    ): ProtoField {
        findDataClass(baseType)?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.MessageRef(baseType, if (isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE),
                nestedMessage = processClass(it),
            )
        }

        findEnumClass(baseType)?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.EnumRef(baseType, if (isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE),
                nestedEnum = analyzeEnum(it),
            )
        }

        return ProtoField(
            name = name,
            number = number,
            fieldType = ProtoFieldType.MessageRef(baseType, ProtoModifier.NONE),
            unresolved = true,
        )
    }

    private fun analyzeEnum(ktClass: KtClass): ProtoEnumModel {
        val entries =
            ktClass.declarations
                .filterIsInstance<KtEnumEntry>()
                .mapNotNull { it.name }
                .toSet()

        return ProtoEnumModel(
            name = ktClass.name!!,
            entries = entries,
        )
    }

    private fun findDataClass(name: String): KtClass? = findKtClass(name)?.takeIf { it.isDataClass() }

    private fun findEnumClass(name: String): KtClass? = findKtClass(name)?.takeIf { it.isEnum() }

    private fun findKtClass(name: String): KtClass? =
        PsiShortNamesCache
            .getInstance(project)
            .getClassesByName(name, scope)
            .filterIsInstance<KtLightClass>()
            .firstNotNullOfOrNull { it.kotlinOrigin as? KtClass }
}
