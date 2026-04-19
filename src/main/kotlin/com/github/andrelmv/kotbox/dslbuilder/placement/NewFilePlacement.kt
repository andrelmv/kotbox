package com.github.andrelmv.kotbox.dslbuilder.placement

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

internal object NewFilePlacement {
    fun insert(
        sourceFile: KtFile,
        fileName: String,
        generatedCode: String,
        project: Project,
    ) {
        val dir = sourceFile.containingDirectory ?: return
        val targetName = if (fileName.endsWith(".kt")) fileName else "$fileName.kt"
        val pkg = sourceFile.packageFqName.asString()
        val header = if (pkg.isNotBlank()) "package $pkg\n\n" else ""

        WriteCommandAction.runWriteCommandAction(project, "Generate DSL Builder", null, {
            val newFile =
                PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(targetName, KotlinFileType.INSTANCE, header + generatedCode)
            dir.add(newFile)
        })
    }
}
