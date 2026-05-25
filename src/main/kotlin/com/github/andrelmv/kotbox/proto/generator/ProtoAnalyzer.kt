package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.utils.isDataClass
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Analyzes a Kotlin data-class hierarchy via PSI and produces a [ProtoMessage]
 * tree that [ProtoRenderer] can later serialize to `.proto` text.
 *
 * **Deduplication**: [processed] stores fully analyzed classes by FQN — prevents duplicate message blocks.
 * Uses FQN as the key to avoid false collisions between same-named classes in different packages.
 *
 * Further enhancement: migrate to K2 Analysis API for even more robust type resolution.
 * The current approach handles explicit imports and same-package resolution correctly.
 * Remaining limitation: wildcard imports (import com.example.*) are not resolved.
 */
internal class ProtoAnalyzer(
    private val project: Project,
    private val scope: GlobalSearchScope,
) {
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
                .filter { it.name != null && it.typeReference != null }
                .mapIndexed { index, param ->
                    resolveField(
                        name = param.name!!,
                        typeReference = param.typeReference!!,
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
        typeReference: KtTypeReference,
        number: Int,
    ): ProtoField {
        val typeText = typeReference.text
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
                        ?.let { findDataClass(it, typeReference) }
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
                        ?.let { findDataClass(it, typeReference) }
                        ?.let(::processClass)

                ProtoField(
                    name = name,
                    number = number,
                    fieldType = ProtoFieldType.Map(resolved.keyProto, resolved.valueProto),
                    nestedMessage = nested,
                )
            }

            null ->
                handleUnmappedType(
                    name = name,
                    number = number,
                    baseType = typeText.trimEnd('?').trim(),
                    isNullable = isNullable,
                    typeReference = typeReference,
                )
        }
    }

    private fun handleUnmappedType(
        name: String,
        number: Int,
        baseType: String,
        isNullable: Boolean,
        typeReference: KtTypeReference,
    ): ProtoField {
        val modifier = if (isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE

        findDataClass(baseType, typeReference)?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.MessageRef(baseType, modifier),
                nestedMessage = processClass(it),
            )
        }

        findEnumClass(baseType, typeReference)?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.EnumRef(baseType, modifier),
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
        return ProtoEnumModel(name = ktClass.name!!, entries = entries)
    }

    private fun findDataClass(
        name: String,
        context: KtTypeReference,
    ): KtClass? = findKtClass(name, context)?.takeIf { it.isDataClass() }

    private fun findEnumClass(
        name: String,
        context: KtTypeReference,
    ): KtClass? = findKtClass(name, context)?.takeIf { it.isEnum() }

    /**
     * Resolves [simpleName] to a [KtClass] using the import context of [context]:
     *
     * 1. Explicit import in the containing file
     * 2. Same package as the containing file
     * 3. Project-wide short name lookup
     */
    private fun findKtClass(
        simpleName: String,
        context: KtTypeReference,
    ): KtClass? {
        val file = context.containingFile as? KtFile

        if (file != null) {
            // 1. Explicit import
            file.importDirectives
                .mapNotNull { it.importedFqName?.asString() }
                .firstOrNull { it.endsWith(".$simpleName") }
                ?.let {
                    val resolved = findKtClassByExactFqn(it)
                    if (resolved != null) return resolved
                }

            // 2. Same package
            val samePackageFqn = "${file.packageFqName.asString()}.$simpleName"
            findKtClassByExactFqn(samePackageFqn)?.let { return it }
        }

        return findKotlinLightClassesByName(simpleName)
            .firstNotNullOfOrNull { it.kotlinOrigin as? KtClass }
    }

    private fun findKtClassByExactFqn(fqn: String): KtClass? {
        val shortName = fqn.substringAfterLast(".")
        return findKotlinLightClassesByName(shortName)
            .mapNotNull { it.kotlinOrigin as? KtClass }
            .firstOrNull { it.fqName?.asString() == fqn }
    }

    private fun findKotlinLightClassesByName(name: String) =
        PsiShortNamesCache
            .getInstance(project)
            .getClassesByName(name, scope)
            .filterIsInstance<KtLightClass>()
}
