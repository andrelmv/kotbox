package com.github.andrelmv.kotbox.proto.generator

internal class ProtoRenderer {
    /**
     * Renders the full `.proto` file content for [model].
     *
     * @param javaPackage  Optional Java/Kotlin package to embed as the
     *                     `option java_package` directive (mirrors the source
     *                     file's package so generated stubs land in the right place).
     */
    fun render(
        model: ProtoMessageModel,
        javaPackage: String = "",
    ): String {
        // First pass: collect all nested messages and build the top-level message
        val messageBlock = renderMessage(model, indent = 0)

        return buildString {
            appendLine("""syntax = "proto3";""")
            appendLine()

            if (javaPackage.isNotBlank()) {
                appendLine("""option java_package = "$javaPackage";""")
                appendLine("""option java_multiple_files = true;""")
                appendLine()
            }

            appendLine("""import "google/protobuf/any.proto";""")
            appendLine()

            append(messageBlock)
        }.trimEnd() + "\n"
    }

    /**
     * Renders one `message` block.  Nested messages are emitted *inside* the
     * enclosing message (proto3 allows nested type definitions), keeping the
     * output self-contained in a single file.
     */
    private fun renderMessage(
        model: ProtoMessageModel,
        indent: Int,
    ): String {
        val pad = TAB.repeat(indent)
        val innerPad = TAB.repeat(indent + 1)

        return buildString {
            // Sibling enums first
            model.fields
                .mapNotNull { it.nestedEnum }
                .distinctBy { it.name }
                .forEach {
                    append(renderEnum(it, indent))
                    appendLine()
                    appendLine()
                }

            // Sibling nested messages
            model.fields
                .mapNotNull { it.nestedMessage }
                .distinctBy { it.name }
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
                        appendLine("// TODO: unresolved type '${field.fieldType}' — update manually")
                        append(innerPad)
                    }

                    val declaration =
                        when (val type = field.fieldType) {
                            is ProtoFieldType.Scalar -> {
                                val prefix = if (type.modifier != ProtoModifier.NONE) "${type.modifier.keyword} " else ""
                                "$prefix${type.protoType} ${field.name.toSnakeCase()} = ${field.number};"
                            }
                            is ProtoFieldType.Repeated ->
                                "repeated ${type.elementProto} ${field.name.toSnakeCase()} = ${field.number};"
                            is ProtoFieldType.Map ->
                                "map<${type.keyProto}, ${type.valueProto}> ${field.name.toSnakeCase()} = ${field.number};"
                        }

                    appendLine(declaration)
                }

            append("$pad}")
        }
    }

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

    /** Converts camelCase identifiers to snake_case per proto style guide. */
    private fun String.toSnakeCase(): String = replace(CAMEL_CASE_REGEX) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase()
}

private const val TAB = "  "
private val CAMEL_CASE_REGEX = Regex("([a-z])([A-Z])")
