package com.github.andrelmv.kotbox.proto.generator

internal sealed interface MappedProtoType {
    data class ScalarType(
        val protoType: String,
        val isNullable: Boolean,
    ) : MappedProtoType

    data class MapType(
        val keyProto: String,
        val valueProto: String, // scalar Proto type OR custom type name
        val isCustomValue: Boolean,
    ) : MappedProtoType

    data class CollectionType(
        val elementProto: String, // scalar Proto type OR custom type name
        val isCustomType: Boolean,
    ) : MappedProtoType
}

internal object ProtoTypeMapper {
    /**
     * Maps a Kotlin type name [type] into a [MappedProtoType].
     *
     * Returns null when the type is a user-defined message
     */
    fun resolve(type: String): MappedProtoType? {
        val kotlinTypeTrimmed = type.trim()

        val isNullable = kotlinTypeTrimmed.endsWith('?')
        val kotlinType = if (isNullable) kotlinTypeTrimmed.dropLast(1) else kotlinTypeTrimmed

        return when {
            kotlinType.isCollection() -> resolveCollectionType(kotlinType)
            kotlinType.isMap() -> resolveMapType(kotlinType)
            else ->
                scalarMap[kotlinType]?.let {
                    MappedProtoType.ScalarType(
                        protoType = it,
                        isNullable = isNullable,
                    )
                }
        }
    }

    private fun resolveCollectionType(kotlinType: String): MappedProtoType.CollectionType {
        val elementKotlin = kotlinType.substringAfter('<').removeSuffix(">").trim()
        val elementProto = scalarMap[elementKotlin]

        return MappedProtoType.CollectionType(
            elementProto = elementProto ?: elementKotlin,
            isCustomType = elementProto == null,
        )
    }

    private fun resolveMapType(kotlinType: String): MappedProtoType.MapType? {
        val inner = kotlinType.removePrefix("Map<").removeSuffix(">")
        val commaIdx = findTopLevelComma(inner).takeIf { it != -1 } ?: return null

        val keyKotlin = inner.substring(0, commaIdx).trim()
        val keyProto = scalarMap[keyKotlin]?.takeIf { it in validProtoMapKeyTypes } ?: return null

        val valueKotlin = inner.substring(commaIdx + 1).trim()
        val valueProto = scalarMap[valueKotlin]

        return MappedProtoType.MapType(
            keyProto = keyProto,
            valueProto = valueProto ?: valueKotlin,
            isCustomValue = valueProto == null,
        )
    }

    private fun String.isCollection() =
        (this.startsWith("List<") || this.startsWith("Set<")) &&
            this.endsWith('>')

    private fun String.isMap() = this.startsWith("Map<") && this.endsWith('>')

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
        s.foldIndexed(0) { i, depth, c ->
            when (c) {
                '<' -> depth + 1
                '>' -> depth - 1
                ',' -> if (depth == 0) return i else depth
                else -> depth
            }
        }
        return -1
    }

    private val scalarMap: Map<String, String> by lazy {
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
    }

    private val validProtoMapKeyTypes by lazy {
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
}
