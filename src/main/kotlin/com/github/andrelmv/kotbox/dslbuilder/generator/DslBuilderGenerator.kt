package com.github.andrelmv.kotbox.dslbuilder.generator

import com.github.andrelmv.kotbox.dslbuilder.DslBuilderPlacementDialog
import com.github.andrelmv.kotbox.dslbuilder.placement.NewFilePlacement
import com.github.andrelmv.kotbox.dslbuilder.placement.PlacementStrategy
import com.github.andrelmv.kotbox.dslbuilder.placement.SameFilePlacement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

object DslBuilderGenerator {
    fun generate(
        project: Project,
        targetClass: KtClass,
    ) {
        if (!targetClass.isDataClass()) {
            showError(project, "'${targetClass.name}' is not a data class.")
            return
        }

        val sourceFile = targetClass.containingFile as? KtFile ?: return

        // Idempotency — check whether a builder already exists
        val builderName = "${targetClass.name}Builder"
        val scope = GlobalSearchScope.projectScope(project)
        val builderExists =
            com.intellij.psi.search.PsiShortNamesCache
                .getInstance(project)
                .getClassesByName(builderName, scope)
                .isNotEmpty()

        if (builderExists) {
            val confirm =
                com.intellij.openapi.ui.Messages.showYesNoDialog(
                    project,
                    "'$builderName' already exists. Overwrite?",
                    "DSL Builder Already Exists",
                    com.intellij.openapi.ui.Messages
                        .getQuestionIcon(),
                )
            if (confirm != com.intellij.openapi.ui.Messages.YES) return
        }

        // Analyze in background — avoids freezing the EDT for large hierarchies
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Analyzing DSL hierarchy") {
                override fun run(indicator: ProgressIndicator) {
                    val analyzer = HierarchyAnalyzer(project, scope)

                    data class AnalysisResult(
                        val hierarchy: BuilderHierarchy,
                        val packageName: String,
                    )
                    val result =
                        try {
                            ApplicationManager.getApplication().runReadAction<AnalysisResult> {
                                AnalysisResult(
                                    hierarchy = analyzer.analyze(targetClass),
                                    packageName = sourceFile.packageFqName.asString(),
                                )
                            }
                        } catch (e: Exception) {
                            ApplicationManager.getApplication().invokeLater {
                                showError(project, "Failed to analyze hierarchy: ${e.message}")
                            }
                            return
                        }

                    val generatedCode = CodeRenderer().render(result.hierarchy, result.packageName)

                    // Placement dialog must run on the EDT
                    ApplicationManager.getApplication().invokeLater {
                        val dialog = DslBuilderPlacementDialog(project, targetClass)
                        if (!dialog.showAndGet()) return@invokeLater

                        when (val strategy = dialog.getPlacementStrategy()) {
                            is PlacementStrategy.SameFile ->
                                SameFilePlacement.insert(sourceFile, generatedCode, project)
                            is PlacementStrategy.NewFile ->
                                NewFilePlacement.insert(sourceFile, strategy.fileName, generatedCode, project)
                        }
                    }
                }
            },
        )
    }

    private fun showError(
        project: Project,
        message: String,
    ) {
        com.intellij.openapi.ui.Messages
            .showErrorDialog(project, message, "DSL Builder Generator")
    }
}
