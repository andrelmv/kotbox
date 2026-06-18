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
    ): String {
        val messageBlock = renderMessage(model, indent = 0)
        val imports = collectWellKnownImports(model)

        return buildString {
            appendLine("""syntax = "proto3";""")
            appendLine()

            if (imports.isNotEmpty()) {
                imports.sorted().forEach { appendLine("""import "$it";""") }
                appendLine()
            }

            if (javaPackage.isNotBlank()) {
                appendLine("""option java_package = "$javaPackage";""")
                appendLine("""option java_multiple_files = true;""")
                appendLine()
            }

            append(messageBlock)
        }.trimEnd() + "\n"
    }

    private fun collectWellKnownImports(model: ProtoMessage): Set<String> =
        model
            .importCandidateTypeNames()
            .mapNotNullTo(sortedSetOf()) { wellKnownImports[it] }

    private fun ProtoMessage.importCandidateTypeNames(): Set<String> {
        val pending = ArrayDeque(listOf(this))
        return generateSequence { pending.removeFirstOrNull() }
            .flatMap { msg -> msg.fields.onEach { it.nestedMessage?.let(pending::add) } }
            .mapNotNull { it.fieldType.importCandidateTypeName() }
            .toSet()
    }

    private fun ProtoFieldType.importCandidateTypeName(): String? =
        when (this) {
            is ProtoFieldType.Scalar -> protoType
            is ProtoFieldType.Repeated -> elementProto
            is ProtoFieldType.Map -> valueProto
            is ProtoFieldType.MessageRef, is ProtoFieldType.EnumRef -> null
        }

    private fun renderMessage(
        model: ProtoMessage,
        indent: Int,
    ): String {
        val pad = TAB.repeat(indent)
        val innerPad = TAB.repeat(indent + 1)

        return buildString {
            model.fields
                .filter { it.nestedEnum != null }
                .distinctBy { it.nestedEnum }
                .forEach {
                    append(renderEnum(it.nestedEnum!!, indent))
                    appendLine()
                    appendLine()
                }

            model.fields
                .mapNotNull { it.nestedMessage }
                .distinctBy { it }
                .forEach {
                    append(renderMessage(it, indent))
                    appendLine()
                    appendLine()
                }

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

    private fun renderEnum(
        model: ProtoEnumModel,
        indent: Int,
    ): String {
        val pad = TAB.repeat(indent)
        val innerPad = TAB.repeat(indent + 1)

        return buildString {
            appendLine("${pad}enum ${model.name} {")
            model.entries.forEachIndexed { index, entry ->
                appendLine("${innerPad}${entry.toSnakeCase().uppercase()} = $index;")
            }
            append("$pad}")
        }
    }
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
