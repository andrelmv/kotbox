package com.github.andrelmv.kotbox.dslbuilder.generator

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Classifies a [KtParameter] into a [BuilderField] using the K2 Analysis API.
 *
 * IMPORTANT: uses the raw typeReference.text (without stripping generics) to detect
 * collection types — isListType/isSetType/isMapType rely on the "<" character.
 * simpleTypeName() is used only for stub-index lookups (short name without generics).
 */
class FieldClassifier(
    private val project: Project,
    private val scope: GlobalSearchScope,
    private val rootClass: KtClass,
) {
    fun classify(param: KtParameter): BuilderField? {
        // rawTypeText: full type text without nullable marker — e.g. "List<String>", "Address"
        val rawTypeText =
            param.typeReference
                ?.text
                ?.removeSuffix("?")
                ?.trim() ?: return null
        // scalarName: base name without generics — used for stub-index lookups
        val scalarName = param.simpleTypeName() ?: return null

        val analysis = param.analyzeK2()
        val isRequired = !analysis.isNullable && !analysis.hasDefault

        return when {
            isListType(rawTypeText) -> classifyList(param, rawTypeText, isRequired)
            isSetType(rawTypeText) -> classifySet(param, rawTypeText, isRequired)
            isMapType(rawTypeText) -> classifyMap(param, rawTypeText, isRequired)
            else -> classifyScalar(param, rawTypeText, scalarName, analysis.isNullable, isRequired)
        }
    }

    private fun classifyScalar(
        param: KtParameter,
        rawTypeName: String,
        scalarName: String,
        isNullable: Boolean,
        isRequired: Boolean,
    ): BuilderField {
        val dataClass = findDataClassInModule(scalarName)
        return if (dataClass != null && isSameModule(dataClass)) {
            BuilderField.NestedBuilder(
                name = param.name ?: "",
                typeName = scalarName,
                builderTypeName = "${scalarName}Builder",
                isRequired = isRequired,
            )
        } else {
            BuilderField.Simple(
                name = param.name ?: "",
                typeName = rawTypeName,
                isNullableInOriginal = isNullable,
                isRequired = isRequired,
            )
        }
    }

    private fun classifyList(
        param: KtParameter,
        rawType: String,
        isRequired: Boolean,
    ): BuilderField {
        val elementType =
            extractGenericArgument(rawType, 0) ?: return BuilderField.Simple(
                name = param.name ?: "",
                typeName = rawType,
                isNullableInOriginal = false,
                isRequired = isRequired,
            )
        val dataClass = findDataClassInModule(elementType.substringBefore("<").trim())
        return if (dataClass != null && isSameModule(dataClass)) {
            BuilderField.NestedBuilderList(
                name = param.name ?: "",
                elementTypeName = elementType,
                elementBuilderTypeName = "${elementType.substringBefore("<").trim()}Builder",
                isRequired = isRequired,
            )
        } else {
            BuilderField.SimpleList(name = param.name ?: "", elementTypeName = elementType, isRequired = isRequired)
        }
    }

    private fun classifySet(
        param: KtParameter,
        rawType: String,
        isRequired: Boolean,
    ): BuilderField {
        val elementType = extractGenericArgument(rawType, 0) ?: "Any"
        return BuilderField.SimpleSet(name = param.name ?: "", elementTypeName = elementType, isRequired = isRequired)
    }

    private fun classifyMap(
        param: KtParameter,
        rawType: String,
        isRequired: Boolean,
    ): BuilderField {
        val keyType = extractGenericArgument(rawType, 0) ?: "Any"
        val valueType = extractGenericArgument(rawType, 1) ?: "Any"
        return BuilderField.SimpleMap(name = param.name ?: "", keyTypeName = keyType, valueTypeName = valueType, isRequired = isRequired)
    }

    /**
     * Looks up a data class by short name in the module via the stub index.
     * IMPORTANT: must be called inside a read action.
     */
    private fun findDataClassInModule(simpleName: String): KtClass? {
        val cleanName = simpleName.substringBefore("<").trim()
        return KotlinClassShortNameIndex[cleanName, project, scope]
            .filterIsInstance<KtClass>()
            .firstOrNull { it.isDataClass() }
    }

    /**
     * Returns true if [target] belongs to the same module as the root class.
     */
    private fun isSameModule(target: KtClass): Boolean {
        val moduleA =
            com.intellij.openapi.module.ModuleUtilCore
                .findModuleForPsiElement(rootClass) ?: return false
        val moduleB =
            com.intellij.openapi.module.ModuleUtilCore
                .findModuleForPsiElement(target) ?: return false
        return moduleA == moduleB
    }

    private fun isListType(t: String) = t.startsWith("List<") || t.startsWith("MutableList<")

    private fun isSetType(t: String) = t.startsWith("Set<") || t.startsWith("MutableSet<")

    private fun isMapType(t: String) = t.startsWith("Map<") || t.startsWith("MutableMap<")

    /**
     * Extracts the N-th generic argument, respecting nested angle brackets.
     * Example: "Map<String, List<Int>>" with index=1 → "List<Int>"
     */
    private fun extractGenericArgument(
        typeName: String,
        index: Int,
    ): String? {
        val start = typeName.indexOf('<')
        val end = typeName.lastIndexOf('>')
        if (start == -1 || end == -1) return null

        val parts = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()
        for (char in typeName.substring(start + 1, end)) {
            when {
                char == '<' -> {
                    depth++
                    current.append(char)
                }
                char == '>' -> {
                    depth--
                    current.append(char)
                }
                char == ',' && depth == 0 -> {
                    parts.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        if (current.isNotBlank()) parts.add(current.toString().trim())
        return parts.getOrNull(index)
    }
}
