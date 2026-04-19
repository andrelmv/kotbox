package com.github.andrelmv.kotbox.dslbuilder.placement

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

internal object SameFilePlacement {
    fun insert(
        file: KtFile,
        generatedCode: String,
        project: Project,
    ) {
        WriteCommandAction.runWriteCommandAction(project, "Generate DSL Builder", null, {
            val factory = KtPsiFactory(project)
            val tempFile = factory.createFile(generatedCode)
            tempFile.declarations.forEach { decl ->
                file.add(factory.createNewLine(2))
                file.add(decl.copy())
            }
        })
    }
}
