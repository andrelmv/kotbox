package com.github.andrelmv.kotbox.proto.generator

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtTypeReference

internal sealed interface MappedType {
    data class ScalarType(
        val type: String,
        val isNullable: Boolean,
    ) : MappedType

    data class MapType(
        val key: String,
        val value: String,
        val customValue: Boolean,
    ) : MappedType

    data class CollectionType(
        val element: String,
        val customElement: Boolean,
    ) : MappedType
}

internal object KotlinToProtoMapper {
    /**
     * Maps a [KtTypeReference] to a [MappedType], first attempting text-based resolution
     * and falling back to K2-based expansion for type aliases.
     */
    fun resolve(typeReference: KtTypeReference): MappedType? = resolve(typeReference.text) ?: resolveFromExpanded(typeReference)

    private fun resolve(type: String): MappedType? {
        val kotlinTypeTrimmed = type.trim()
        val isNullable = kotlinTypeTrimmed.endsWith('?')
        val kotlinType = if (isNullable) kotlinTypeTrimmed.dropLast(1) else kotlinTypeTrimmed

        return when {
            kotlinType.isCollection() -> resolveCollectionType(kotlinType)
            kotlinType.isMap() -> resolveMapType(kotlinType)
            else -> scalarMap[kotlinType]?.let { MappedType.ScalarType(type = it, isNullable = isNullable) }
        }
    }

    /**
     * K2-based fallback for type alias resolution. Expands the type alias and maps
     * the underlying type — handles scalar aliases, collection aliases, and map aliases.
     */
    @OptIn(KaExperimentalApi::class)
    private fun resolveFromExpanded(typeReference: KtTypeReference): MappedType? =
        analyze(typeReference) {
            val expanded = typeReference.type.fullyExpandedType
            val isNullable = expanded.isMarkedNullable
            val classType = expanded as? KaClassType ?: return@analyze null
            when (val shortName = classType.classId.shortClassName.asString()) {
                "List", "Set" -> {
                    val elementShort = typeArgumentShortName(classType, 0) ?: return@analyze null
                    val elementProto = scalarProto(elementShort)
                    MappedType.CollectionType(
                        element = elementProto ?: elementShort,
                        customElement = elementProto == null,
                    )
                }
                "Map" -> {
                    val keyShort = typeArgumentShortName(classType, 0) ?: return@analyze null
                    val valueShort = typeArgumentShortName(classType, 1) ?: return@analyze null
                    val keyProto =
                        scalarProto(keyShort)
                            ?.takeIf { it in validProtoMapKeyTypes }
                            ?: return@analyze null
                    val valueProto = scalarProto(valueShort)
                    MappedType.MapType(
                        key = keyProto,
                        value = valueProto ?: valueShort,
                        customValue = valueProto == null,
                    )
                }
                else -> {
                    val proto = scalarProto(shortName) ?: return@analyze null
                    MappedType.ScalarType(type = proto, isNullable = isNullable)
                }
            }
        }

    private fun scalarProto(kotlinShortName: String): String? = scalarMap[kotlinShortName]

    private fun resolveCollectionType(kotlinType: String): MappedType.CollectionType {
        val elementKotlin = kotlinType.substringAfter('<').removeSuffix(">").trim()
        val elementProto = scalarMap[elementKotlin]
        return MappedType.CollectionType(
            element = elementProto ?: elementKotlin,
            customElement = elementProto == null,
        )
    }

    private fun resolveMapType(kotlinType: String): MappedType.MapType? {
        val inner = kotlinType.removePrefix("Map<").removeSuffix(">")
        val commaIdx = findTopLevelComma(inner).takeIf { it != -1 } ?: return null

        val keyKotlin = inner.substring(0, commaIdx).trim()
        val keyProto = scalarMap[keyKotlin]?.takeIf { it in validProtoMapKeyTypes } ?: return null

        val valueKotlin = inner.substring(commaIdx + 1).trim()
        val valueProto = scalarMap[valueKotlin]
        return MappedType.MapType(
            key = keyProto,
            value = valueProto ?: valueKotlin,
            customValue = valueProto == null,
        )
    }

    private fun String.isCollection() = (startsWith("List<") || startsWith("Set<")) && endsWith('>')

    private fun String.isMap() = startsWith("Map<") && endsWith('>')

    /**
     * Finds the index of the first comma in [s] that is not nested inside angle brackets.
     *
     * Used to split a `Map<K, V>` type string into its key and value components.
     *
     * Examples:
     * - `"String, Int"` -> 6
     * - `"String, List<Int>"` -> 6 (the comma inside List<> is ignored)
     * - `"String"` -> -1 (no top-level comma found)
     *
     * @return the index of the top-level comma, or -1 if none exists.
     */
    private fun findTopLevelComma(s: String): Int {
        var depth = 0
        for ((i, c) in s.withIndex()) {
            when (c) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) return i
            }
        }
        return -1
    }

    private val scalarMap: Map<String, String> =
        mapOf(
            "String" to "string",
            "Int" to "int32",
            "Long" to "int64",
            "Short" to "int32",
            "Byte" to "int32",
            "Float" to "float",
            "Double" to "double",
            "Boolean" to "bool",
            "ByteArray" to "bytes",
            "Any" to "google.protobuf.Any",
        )

    private val validProtoMapKeyTypes =
        setOf(
            "string",
            "int32",
            "int64",
            "uint32",
            "uint64",
            "sint32",
            "sint64",
            "fixed32",
            "fixed64",
            "sfixed32",
            "sfixed64",
            "bool",
        )
}

/**
 * Expands the type argument at [index] and returns its short class name, or null
 */
@OptIn(KaExperimentalApi::class)
private fun KaSession.typeArgumentShortName(
    classType: KaClassType,
    index: Int,
): String? =
    classType.typeArguments
        .getOrNull(index)
        ?.type
        ?.fullyExpandedType
        ?.shortName()

private fun KaType.shortName(): String? = (this as? KaClassType)?.classId?.shortClassName?.asString()
