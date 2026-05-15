package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.utils.isDataClass
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry

/**
 * Analyzes a Kotlin data-class hierarchy via PSI and produces a [ProtoMessageModel]
 * tree that [ProtoRenderer] can later serialize to `.proto` text.
 * Cycle detection is handled via a visited set — never remove that guard.
 *
 * Responsibility:
 * - Walking the PSI tree and extracting type information
 * - Building a ProtoMessageModel tree
 *
 */
internal class ProtoAnalyzer(
    private val project: Project,
    private val scope: GlobalSearchScope,
) {
    // Tracks classes currently being visited to detect direct cycles.
    private val currentlyProcessing = mutableSetOf<String>()

    fun analyze(rootClass: KtClass): ProtoMessageModel {
        require(rootClass.isDataClass()) { "'${rootClass.name}' is not a data class" }
        return processClass(rootClass)
    }

    private fun processClass(ktClass: KtClass): ProtoMessageModel {
        val className = ktClass.name!!
        currentlyProcessing.add(className)

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

        currentlyProcessing.remove(className)

        return ProtoMessageModel(
            name = className,
            fields = fields,
        )
    }

    private fun resolveField(
        name: String,
        typeText: String,
        number: Int,
    ): ProtoField {
        val isNullable = typeText.endsWith('?')
        return when (val resolved = ProtoTypeMapper.resolve(typeText)) {
            is MappedProtoType.Scalar ->
                field {
                    this.name = name
                    this.number = number
                    this.fieldType =
                        ProtoFieldType.Scalar(
                            protoType = resolved.protoType,
                            modifier =
                                if (resolved.isNullable) {
                                    ProtoModifier.OPTIONAL
                                } else {
                                    ProtoModifier.NONE
                                },
                        )
                }

            is MappedProtoType.CollectionType -> {
                val nested =
                    resolved.elementProto
                        .takeIf { resolved.isCustomType && it !in currentlyProcessing }
                        ?.let(::findDataClass)
                        ?.let(::processClass)

                field {
                    this.name = name
                    this.number = number
                    this.fieldType = ProtoFieldType.Repeated(resolved.elementProto)
                    this.nestedMessage = nested
                }
            }

            is MappedProtoType.MapType -> {
                val nested =
                    resolved.valueProto
                        .takeIf { resolved.isCustomValue && it !in currentlyProcessing }
                        ?.let(::findDataClass)
                        ?.let(this::processClass)

                field {
                    this.name = name
                    this.number = number
                    this.fieldType = ProtoFieldType.Map(resolved.keyProto, resolved.valueProto)
                    this.nestedMessage = nested
                }
            }

            null -> {
                val baseType = typeText.trimEnd('?').trim()
                findDataClass(baseType)?.takeIf { baseType !in currentlyProcessing }?.let {
                    return field {
                        this.name = name
                        this.number = number
                        this.fieldType =
                            ProtoFieldType.Scalar(
                                protoType = baseType,
                                modifier =
                                    if (isNullable) {
                                        ProtoModifier.OPTIONAL
                                    } else {
                                        ProtoModifier.NONE
                                    },
                            )
                        this.nestedMessage = processClass(it)
                    }
                }

                findEnumClass(baseType)?.let {
                    return field {
                        this.name = name
                        this.number = number
                        this.fieldType =
                            ProtoFieldType.Scalar(
                                protoType = baseType,
                                modifier =
                                    if (isNullable) {
                                        ProtoModifier.OPTIONAL
                                    } else {
                                        ProtoModifier.NONE
                                    },
                            )
                        this.nestedEnum = analyzeEnum(it)
                    }
                }

                field {
                    this.name = name
                    this.number = number
                    this.fieldType =
                        ProtoFieldType.Scalar(
                            protoType = baseType,
                            modifier = ProtoModifier.NONE,
                        )
                    this.unresolved = true
                }
            }
        }
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
