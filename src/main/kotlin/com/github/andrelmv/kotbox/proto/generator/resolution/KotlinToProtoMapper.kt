package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtTypeReference

internal object KotlinToProtoMapper {
    /**
     * Text-only mapping; resolves scalars, collections, and maps without any compiler analysis.
     */
    fun resolveFromText(type: String): ProtoTypeMapping? {
        val kotlinTypeTrimmed = type.trim()
        val isNullable = kotlinTypeTrimmed.endsWith('?')
        val kotlinType = if (isNullable) kotlinTypeTrimmed.dropLast(1) else kotlinTypeTrimmed

        return when {
            kotlinType.isCollection() -> resolveCollectionTypeMapping(kotlinType)
            kotlinType.isMap() -> resolveMapTypeMapping(kotlinType)
            else ->
                scalarMapping(
                    kotlinShortName = kotlinType,
                    isNullable = isNullable,
                )
        }
    }

    /**
     * K2-based fallback for type alias resolution. Expands the type alias and maps
     * the underlying type — handles scalar aliases, collection aliases, and map aliases.
     */
    @OptIn(KaExperimentalApi::class)
    fun KaSession.resolveExpanded(typeReference: KtTypeReference): ProtoTypeMapping? {
        val classType = expandedClassType(typeReference) ?: return null
        val isNullable = classType.isMarkedNullable
        return when (val shortName = classType.shortName) {
            "List", "Set" -> {
                val elementShort = typeArgumentShortName(classType, 0) ?: return null
                collectionMapping(elementShort)
            }
            "Map" -> {
                val keyShort = typeArgumentShortName(classType, 0) ?: return null
                val valueShort = typeArgumentShortName(classType, 1) ?: return null
                mapMapping(keyShort, valueShort)
            }
            else ->
                scalarMapping(
                    kotlinShortName = shortName,
                    isNullable = isNullable,
                )
        }
    }

    private fun scalarProto(kotlinShortName: String): String? = scalarMap[kotlinShortName]

    /**
     * Builds a [ProtoTypeMapping.ScalarTypeMapping], or null if [kotlinShortName] isn't a known scalar.
     */
    private fun scalarMapping(
        kotlinShortName: String,
        isNullable: Boolean,
    ): ProtoTypeMapping.ScalarTypeMapping? =
        scalarProto(kotlinShortName)?.let {
            ProtoTypeMapping.ScalarTypeMapping(type = it, isNullable = isNullable)
        }

    /**
     * Builds a [ProtoTypeMapping.CollectionTypeMapping], falling back to [elementShortName] for custom element types.
     */
    private fun collectionMapping(elementShortName: String): ProtoTypeMapping.CollectionTypeMapping {
        val elementProto = scalarProto(elementShortName)
        return ProtoTypeMapping.CollectionTypeMapping(
            element = elementProto ?: elementShortName,
            customElement = elementProto == null,
        )
    }

    /**
     * Builds a [ProtoTypeMapping.MapTypeMapping], or null if [keyShortName] isn't a valid proto map key type.
     */
    private fun mapMapping(
        keyShortName: String,
        valueShortName: String,
    ): ProtoTypeMapping.MapTypeMapping? {
        val keyProto = scalarProto(keyShortName)?.takeIf { it in validProtoMapKeyTypes } ?: return null
        val valueProto = scalarProto(valueShortName)
        return ProtoTypeMapping.MapTypeMapping(
            key = keyProto,
            value = valueProto ?: valueShortName,
            customValue = valueProto == null,
        )
    }

    private fun resolveCollectionTypeMapping(kotlinType: String): ProtoTypeMapping.CollectionTypeMapping {
        val elementKotlin = kotlinType.substringAfter('<').removeSuffix(">").trim()
        return collectionMapping(elementKotlin)
    }

    private fun resolveMapTypeMapping(kotlinType: String): ProtoTypeMapping.MapTypeMapping? {
        val inner = kotlinType.removePrefix("Map<").removeSuffix(">")
        val commaIdx = findTopLevelComma(inner).takeIf { it != -1 } ?: return null

        val keyKotlin = inner.substring(0, commaIdx).trim()
        val valueKotlin = inner.substring(commaIdx + 1).trim()
        return mapMapping(keyKotlin, valueKotlin)
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

private fun KaSession.typeArgumentShortName(
    classType: KaClassType,
    index: Int,
): String? = expandedTypeArgument(classType, index)?.shortName
