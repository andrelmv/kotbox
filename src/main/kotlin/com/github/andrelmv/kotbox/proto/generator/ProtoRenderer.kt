package com.github.andrelmv.kotbox.proto.generator

internal object ProtoRenderer {
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
                .filter { it.fieldType is ProtoFieldType.EnumRef }
                .distinctBy { it.nestedEnum!! }
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
                        appendLine("// TODO: unresolved type '${field.fieldType}' — update manually")
                        append(innerPad)
                    }

                    val declaration =
                        when (val type = field.fieldType) {
                            is ProtoFieldType.Scalar -> {
                                val prefix = type.modifier.toPrefix()
                                "$prefix${type.protoType} ${field.name.toSnakeCase()} = ${field.number};"
                            }
                            is ProtoFieldType.MessageRef -> {
                                val prefix = type.modifier.toPrefix()
                                "${prefix}${type.typeName} ${field.name.toSnakeCase()} = ${field.number};"
                            }
                            is ProtoFieldType.Repeated -> "repeated ${type.elementProto} ${field.name.toSnakeCase()} = ${field.number};"
                            is ProtoFieldType.Map -> {
                                "map<${type.keyProto}, ${type.valueProto}> ${field.name.toSnakeCase()} = ${field.number};"
                            }
                            is ProtoFieldType.EnumRef -> {
                                val prefix = type.modifier.toPrefix()
                                "$prefix${type.typeName} ${field.name.toSnakeCase()} = ${field.number};"
                            }
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

    // Converts camelCase identifiers to snake_case per proto style guide
    private fun String.toSnakeCase(): String = this.replace(CAMEL_CASE_REGEX) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase()

    private fun ProtoModifier.toPrefix() = if (this != ProtoModifier.NONE) "${this.keyword} " else ""
}

// 2 spaces, proto style guide
private const val TAB = "  "
private val CAMEL_CASE_REGEX = Regex("([a-z])([A-Z])")
