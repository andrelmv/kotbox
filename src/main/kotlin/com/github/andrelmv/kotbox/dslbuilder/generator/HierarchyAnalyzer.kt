package com.github.andrelmv.kotbox.dslbuilder.generator

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

/**
 * Analyzes the data class hierarchy starting from a root class and produces
 * a [BuilderHierarchy] in topological order (dependencies before dependents).
 * Uses [FieldClassifier] (backed by the K2 Analysis API) to classify each field.
 * Cycle detection is handled via a visited set — never remove that guard.
 */
class HierarchyAnalyzer(
    private val project: Project,
    private val scope: GlobalSearchScope,
) {
    fun analyze(rootClass: KtClass): BuilderHierarchy {
        require(rootClass.isDataClass()) { "'${rootClass.name}' is not a data class" }

        val rootName = rootClass.name ?: error("Root class has no name")
        val dslMarkerName = "${rootName}Dsl"
        val classifier = FieldClassifier(project, scope, rootClass)

        val visited = mutableSetOf<String>()
        val orderedBuilders = mutableListOf<BuilderClassModel>()
        val requiredImports = mutableSetOf<String>()

        fun processClass(
            ktClass: KtClass,
            isRoot: Boolean,
        ) {
            val className = ktClass.name ?: return
            if (className in visited) return
            visited.add(className)

            val params = ktClass.primaryConstructorParameters
            val fields = params.mapNotNull { classifier.classify(it) }

            // Process dependencies first (DFS — guarantees topological order)
            fields
                .filterIsInstance<BuilderField.NestedBuilder>()
                .forEach { findDataClass(it.typeName)?.let { cls -> processClass(cls, false) } }
            fields
                .filterIsInstance<BuilderField.NestedBuilderList>()
                .forEach { findDataClass(it.elementTypeName)?.let { cls -> processClass(cls, false) } }

            // Collect imports for nested types
            fields.forEach { field ->
                when (field) {
                    is BuilderField.NestedBuilder -> collectImport(field.typeName, requiredImports)
                    is BuilderField.NestedBuilderList -> collectImport(field.elementTypeName, requiredImports)
                    else -> {}
                }
            }

            val packageName =
                (ktClass.containingFile as? KtFile)
                    ?.packageFqName
                    ?.asString() ?: ""

            orderedBuilders.add(
                BuilderClassModel(
                    dataClassName = className,
                    builderClassName = "${className}Builder",
                    packageName = packageName,
                    fields = fields,
                    dslMarkerName = dslMarkerName,
                    isRoot = isRoot,
                ),
            )
        }

        processClass(rootClass, isRoot = true)

        return BuilderHierarchy(
            builders = orderedBuilders,
            dslMarkerName = dslMarkerName,
            requiredImports = requiredImports,
        )
    }

    private fun findDataClass(simpleName: String): KtClass? {
        val cleanName = simpleName.substringBefore("<").trim()
        return KotlinClassShortNameIndex
            .get(cleanName, project, scope)
            .filterIsInstance<KtClass>()
            .firstOrNull { it.isDataClass() }
    }

    private fun collectImport(
        className: String,
        imports: MutableSet<String>,
    ) {
        val cls = findDataClass(className) ?: return
        val pkg = (cls.containingFile as? KtFile)?.packageFqName?.asString() ?: return
        if (pkg.isNotBlank()) imports.add("$pkg.$className")
    }
}
