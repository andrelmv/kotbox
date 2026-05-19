package com.github.andrelmv.kotbox.proto.dialog

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
        WriteCommandAction.runWriteCommandAction(project, "Generate Proto", null, {
            val newFile =
                PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(fileName, KotlinFileType.INSTANCE, generatedCode)
            dir.add(newFile)
        })
    }
}
