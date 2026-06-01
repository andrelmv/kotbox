package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.proto.dialog.NewFilePlacement
import com.github.andrelmv.kotbox.proto.dialog.PlacementDialog
import com.github.andrelmv.kotbox.proto.dialog.PlacementStrategy
import com.github.andrelmv.kotbox.proto.dialog.PreviewDialog
import com.github.andrelmv.kotbox.utils.isDataClass
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

internal object ProtoGenerator {
    fun generate(
        project: Project,
        targetClass: KtClass,
    ) {
        if (!targetClass.isDataClass()) {
            showError(project, "'${targetClass.name}' is not a data class.")
            return
        }

        val sourceFile = targetClass.containingFile as? KtFile ?: return
        val scope = GlobalSearchScope.projectScope(project)

        // Analyze in background — avoids freezing the EDT for large hierarchies
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Analyzing class for proto generation") {
                override fun run(indicator: ProgressIndicator) {
                    val resolver = KotlinClassFinder(project, scope)
                    val analyzer = ClassAnalyzer(resolver)

                    data class AnalysisResult(
                        val protoText: String,
                    )

                    val result =
                        try {
                            ApplicationManager
                                .getApplication()
                                .runReadAction<AnalysisResult> {
                                    val model = analyzer.analyze(targetClass)
                                    AnalysisResult(
                                        protoText =
                                            CodeRenderer.render(
                                                model = model,
                                                javaPackage = sourceFile.packageFqName.asString(),
                                            ),
                                    )
                                }
                        } catch (e: Exception) {
                            ApplicationManager.getApplication().invokeLater {
                                showError(project, "Failed to analyze class: ${e.message}")
                            }
                            return
                        }

                    // Placement dialog must run on the EDT
                    ApplicationManager.getApplication().invokeLater {
                        val dialog = PlacementDialog("Proto", project, targetClass.name!!)
                        if (!dialog.showAndGet()) return@invokeLater

                        when (val strategy = dialog.getPlacementStrategy()) {
                            is PlacementStrategy.PreviewAndCopy ->
                                PreviewDialog(project, result.protoText).show()
                            is PlacementStrategy.NewFile ->
                                NewFilePlacement.insert(
                                    sourceFile,
                                    strategy.fileName,
                                    result.protoText,
                                    project,
                                )
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
            .showErrorDialog(project, message, "Proto Generator")
    }
}
