package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.utils.isDataClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Analyzes a Kotlin data-class hierarchy and produces a [ProtoMessage] tree.
 * Classes are cached by FQN to handle duplicate references and thus same-named class collisions that would result in duplicate message blocks.
 */
internal class ClassAnalyzer(
    private val finder: KotlinClassFinder,
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

        return when (val mappedType = KotlinToProtoMapper.resolve(typeText)) {
            is MappedType.ScalarType ->
                ProtoField(
                    name = name,
                    number = number,
                    fieldType =
                        ProtoFieldType.Scalar(
                            protoType = mappedType.type,
                            modifier = if (mappedType.isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE,
                        ),
                )

            is MappedType.CollectionType -> {
                val nested =
                    mappedType.element
                        .takeIf { mappedType.customElement }
                        ?.let { finder.findDataClass(it, typeReference) }
                        ?.let(::processClass)

                ProtoField(
                    name = name,
                    number = number,
                    fieldType = ProtoFieldType.Repeated(mappedType.element),
                    nestedMessage = nested,
                )
            }

            is MappedType.MapType -> {
                val nested =
                    mappedType.value
                        .takeIf { mappedType.customValue }
                        ?.let { finder.findDataClass(it, typeReference) }
                        ?.let(::processClass)

                ProtoField(
                    name = name,
                    number = number,
                    fieldType = ProtoFieldType.Map(mappedType.key, mappedType.value),
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

        finder.findDataClass(baseType, typeReference)?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.MessageRef(baseType, modifier),
                nestedMessage = processClass(it),
            )
        }

        finder.findEnumClass(baseType, typeReference)?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.EnumRef(baseType, modifier),
                nestedEnum = it.toProtoEnumModel(),
            )
        }

        return ProtoField(
            name = name,
            number = number,
            fieldType = ProtoFieldType.MessageRef(baseType, ProtoModifier.NONE),
            unresolved = true,
        )
    }

    private fun KtClass.toProtoEnumModel(): ProtoEnumModel {
        val entries =
            this.declarations
                .filterIsInstance<KtEnumEntry>()
                .mapNotNull { it.name }
                .let { LinkedHashSet(it) }
        return ProtoEnumModel(name = this.name!!, entries = entries)
    }
}
