package com.github.andrelmv.kotbox.dslbuilder.generator

/**
 * Renders a [BuilderHierarchy] as Kotlin source text.
 * No IntelliJ dependencies — testable as plain JUnit.
 *
 * Emits import statements when [BuilderHierarchy.requiredImports] is non-empty,
 * skipping any type that already lives in the same-package (same-package imports are redundant).
 */
class CodeRenderer {
    fun render(
        hierarchy: BuilderHierarchy,
        ownPackage: String = "",
    ): String {
        val sb = StringBuilder()

        // Imports — skip types in the same package as the generated file
        val imports =
            hierarchy.requiredImports
                .filter { fqn -> ownPackage.isBlank() || fqn.substringBeforeLast(".") != ownPackage }
                .sorted()
        if (imports.isNotEmpty()) {
            imports.forEach { sb.appendLine("import $it") }
            sb.appendLine()
        }

        sb.appendLine(renderDslMarker(hierarchy.dslMarkerName))
        sb.appendLine()
        hierarchy.builders.forEach { builder ->
            sb.appendLine(renderBuilder(builder))
            sb.appendLine()
        }
        hierarchy.builders.lastOrNull { it.isRoot }?.let {
            sb.appendLine(renderEntryFunction(it))
        }
        return sb.toString().trimEnd()
    }

    private fun renderDslMarker(name: String) = "@DslMarker\nannotation class $name"

    private fun renderBuilder(model: BuilderClassModel): String {
        val sb = StringBuilder()
        sb.appendLine("@${model.dslMarkerName}")
        sb.appendLine("class ${model.builderClassName} {")
        model.fields.forEach { sb.appendLine(renderField(it).prependIndent("    ")) }
        if (model.fields.isNotEmpty()) sb.appendLine()

        val dslMethods =
            model.fields.filter {
                it is BuilderField.NestedBuilder ||
                    it is BuilderField.NestedBuilderList ||
                    it is BuilderField.SimpleList ||
                    it is BuilderField.SimpleSet ||
                    it is BuilderField.SimpleMap
            }
        dslMethods.forEach { sb.appendLine(renderDslMethod(it).prependIndent("    ")) }
        if (dslMethods.isNotEmpty()) sb.appendLine()

        sb.appendLine(renderBuildMethod(model).prependIndent("    "))
        sb.append("}")
        return sb.toString()
    }

    /**
     * Produces a nullable form of typeName.
     * Function types like "(String, Int) -> String" must be wrapped in parens first,
     * otherwise "? " binds to the return type: "(String, Int) -> String?" is wrong,
     * "((String, Int) -> String)?" is correct.
     */
    private fun nullableType(typeName: String): String = if (typeName.contains("->")) "($typeName)?" else "$typeName?"

    private fun renderField(field: BuilderField): String =
        when (field) {
            is BuilderField.Simple -> "var ${field.name}: ${nullableType(field.typeName)} = null"
            is BuilderField.NestedBuilder -> "private var ${field.name}: ${nullableType(field.typeName)} = null"
            is BuilderField.SimpleList -> "private val ${field.name}: MutableList<${field.elementTypeName}> = mutableListOf()"
            is BuilderField.NestedBuilderList -> "private val ${field.name}: MutableList<${field.elementTypeName}> = mutableListOf()"
            is BuilderField.SimpleSet -> "private val ${field.name}: MutableSet<${field.elementTypeName}> = mutableSetOf()"
            is BuilderField.SimpleMap ->
                "private val ${field.name}: MutableMap<${field.keyTypeName}, ${field.valueTypeName}> = mutableMapOf()"
        }

    private fun renderDslMethod(field: BuilderField): String =
        when (field) {
            is BuilderField.NestedBuilder ->
                """
                fun ${field.name}(block: ${field.builderTypeName}.() -> Unit) {
                    ${field.name} = ${field.builderTypeName}().apply(block).build()
                }
                """.trimIndent()

            is BuilderField.NestedBuilderList ->
                """
                fun ${field.name}(block: ${field.elementBuilderTypeName}.() -> Unit) {
                    ${field.name}.add(${field.elementBuilderTypeName}().apply(block).build())
                }
                """.trimIndent()

            is BuilderField.SimpleList ->
                """
                fun ${field.name}(vararg items: ${field.elementTypeName}) {
                    ${field.name}.addAll(items.toList())
                }
                """.trimIndent()

            is BuilderField.SimpleSet ->
                """
                fun ${field.name}(vararg items: ${field.elementTypeName}) {
                    ${field.name}.addAll(items.toList())
                }
                """.trimIndent()

            is BuilderField.SimpleMap ->
                """
                fun ${field.name}(key: ${field.keyTypeName}, value: ${field.valueTypeName}) {
                    ${field.name}[key] = value
                }
                """.trimIndent()

            else -> ""
        }

    private fun renderBuildMethod(model: BuilderClassModel): String {
        val sb = StringBuilder()
        sb.appendLine("fun build(): ${model.dataClassName} = ${model.dataClassName}(")
        model.fields.forEach { sb.appendLine(renderBuildArgument(it, model.builderClassName).prependIndent("    ")) }
        sb.append(")")
        return sb.toString()
    }

    private fun renderBuildArgument(
        field: BuilderField,
        builderName: String,
    ): String {
        val n = field.name
        return when (field) {
            is BuilderField.Simple -> if (field.isRequired) "$n = $n ?: error(\"$builderName: '$n' is required\")," else "$n = $n,"
            is BuilderField.NestedBuilder -> if (field.isRequired) "$n = $n ?: error(\"$builderName: '$n' is required\")," else "$n = $n,"
            is BuilderField.SimpleList -> "$n = $n.toList(),"
            is BuilderField.NestedBuilderList -> "$n = $n.toList(),"
            is BuilderField.SimpleSet -> "$n = $n.toSet(),"
            is BuilderField.SimpleMap -> "$n = $n.toMap(),"
        }
    }

    private fun renderEntryFunction(model: BuilderClassModel): String {
        val funcName = model.dataClassName.replaceFirstChar { it.lowercaseChar() }
        return "fun $funcName(block: ${model.builderClassName}.() -> Unit): ${model.dataClassName} " +
            "=\n    ${model.builderClassName}().apply(block).build()"
    }
}
