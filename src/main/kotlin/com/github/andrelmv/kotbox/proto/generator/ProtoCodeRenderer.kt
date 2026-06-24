package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier

internal object ProtoCodeRenderer {
    fun render(
        model: ProtoMessage,
        javaPackage: String = "",
    ): String =
        buildString {
            appendLine("""syntax = "proto3";""")
            appendLine()

            val protoFields = model.flattenedFields()

            protoFields
                .collectWellKnownImports()
                .sorted()
                .forEach { appendLine("""import "$it";""") }
            appendLine()

            if (javaPackage.isNotBlank()) {
                appendLine("""option java_package = "$javaPackage";""")
                appendLine("""option java_multiple_files = true;""")
                appendLine()
            }

            protoFields
                .collectEnums()
                .forEach {
                    appendLine(renderEnum(it))
                    appendLine()
                }

            collectMessages(model, protoFields).forEach {
                appendLine(renderMessage(it))
                appendLine()
            }
        }.trimEnd() + "\n"

    private fun ProtoMessage.flattenedFields(): List<ProtoField> {
        val pending = ArrayDeque(listOf(this))
        return generateSequence { pending.removeFirstOrNull() }
            .flatMap { it.fields }
            .onEach { it.nestedMessage?.let(pending::add) }
            .toList()
    }

    private fun List<ProtoField>.collectWellKnownImports(): Set<String> =
        mapNotNullTo(sortedSetOf()) {
            it.fieldType
                .wellKnownTypeName()
                ?.let(wellKnownImports::get)
        }

    private fun ProtoFieldType.wellKnownTypeName(): String? =
        when (this) {
            is ProtoFieldType.Scalar -> protoType
            is ProtoFieldType.Repeated -> elementProto
            is ProtoFieldType.Map -> valueProto
            is ProtoFieldType.MessageRef, is ProtoFieldType.EnumRef -> null
        }

    private fun List<ProtoField>.collectEnums(): LinkedHashSet<ProtoEnumModel> = mapNotNull { it.nestedEnum }.toCollection(LinkedHashSet())

    private fun collectMessages(
        model: ProtoMessage,
        fields: List<ProtoField>,
    ): LinkedHashSet<ProtoMessage> =
        (listOf(model) + fields.mapNotNull { it.nestedMessage })
            .asReversed()
            .toCollection(LinkedHashSet())

    private fun renderEnum(model: ProtoEnumModel): String =
        buildString {
            appendLine("enum ${model.name} {")
            model.entries
                .forEachIndexed { index, entry ->
                    appendLine("$TAB${entry.toSnakeCase().uppercase()} = $index;")
                }
            append("}")
        }

    private fun renderMessage(
        model: ProtoMessage,
        indent: Int = 0,
    ): String {
        val pad = TAB.repeat(indent)
        val innerPad = TAB.repeat(indent + 1)

        return buildString {
            appendLine("${pad}message ${model.name} {")

            model.fields
                .forEach { field ->
                    append(innerPad)

                    if (field.unresolved) {
                        appendLine("// TODO: unresolved type '${renderType(field.fieldType)}' — update manually")
                        append(innerPad)
                    }

                    appendLine(renderField(field))
                }

            append("$pad}")
        }
    }

    private fun renderField(field: ProtoField): String = "${renderType(field.fieldType)} ${field.name.toSnakeCase()} = ${field.number};"

    private fun renderType(type: ProtoFieldType): String =
        when (type) {
            is ProtoFieldType.Scalar -> "${type.modifier.prefix()}${type.protoType}"
            is ProtoFieldType.Repeated -> "repeated ${type.elementProto}"
            is ProtoFieldType.Map -> "map<${type.keyProto}, ${type.valueProto}>"
            is ProtoFieldType.MessageRef -> "${type.modifier.prefix()}${type.typeName}"
            is ProtoFieldType.EnumRef -> "${type.modifier.prefix()}${type.typeName}"
        }

    private fun ProtoModifier.prefix(): String = if (this == ProtoModifier.OPTIONAL) "optional " else ""
}

// 2 spaces, proto style guide
private const val TAB = "  "
private val CAMEL_CASE_REGEX = Regex("([a-z])([A-Z])")
private val wellKnownImports: Map<String, String> =
    mapOf(
        "google.protobuf.Timestamp" to "google/protobuf/timestamp.proto",
        "google.protobuf.Any" to "google/protobuf/any.proto",
    )

private fun String.toSnakeCase(): String = this.replace(CAMEL_CASE_REGEX) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase()
