package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier

internal object ProtoCodeRenderer {
    /**
     * Renders the full `.proto` file content for [model].
     *
     * @param javaPackage  Optional Java/Kotlin package to embed as the
     *                     `option java_package` directive (mirrors the source
     *                     file's package so generated stubs land in the right place).
     */
    fun render(
        model: ProtoMessage,
        javaPackage: String = "",
    ): String {
        val messageBlock = renderMessage(model, indent = 0)

        return buildString {
            appendLine("""syntax = "proto3";""")
            appendLine()

            if (javaPackage.isNotBlank()) {
                appendLine("""option java_package = "$javaPackage";""")
                appendLine("""option java_multiple_files = true;""")
                appendLine()
            }

            append(messageBlock)
        }.trimEnd() + "\n"
    }

    private fun renderMessage(
        model: ProtoMessage,
        indent: Int,
    ): String {
        val pad = TAB.repeat(indent)
        val innerPad = TAB.repeat(indent + 1)

        return buildString {
            // Sibling enums first
            model.fields
                .filter { it.fieldType is ProtoFieldType.EnumRef && it.nestedEnum != null }
                .distinctBy { it.nestedEnum }
                .forEach {
                    append(renderEnum(it.nestedEnum!!, indent))
                    appendLine()
                    appendLine()
                }

            // Sibling nested messages
            model.fields
                .mapNotNull { it.nestedMessage }
                .distinctBy { it }
                .forEach {
                    append(renderMessage(it, indent))
                    appendLine()
                    appendLine()
                }

            appendLine("${pad}message ${model.name} {")

            // Field declarations
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
            is ProtoFieldType.Scalar -> "${prefix(type.modifier)}${type.protoType}"
            is ProtoFieldType.Repeated -> "repeated ${type.elementProto}"
            is ProtoFieldType.Map -> "map<${type.keyProto}, ${type.valueProto}>"
            is ProtoFieldType.MessageRef -> "${prefix(type.modifier)}${type.typeName}"
            is ProtoFieldType.EnumRef -> "${prefix(type.modifier)}${type.typeName}"
        }

    private fun prefix(modifier: ProtoModifier): String = if (modifier == ProtoModifier.OPTIONAL) "optional " else ""

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

internal fun String.toSnakeCase(): String = this.replace(CAMEL_CASE_REGEX) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase()
