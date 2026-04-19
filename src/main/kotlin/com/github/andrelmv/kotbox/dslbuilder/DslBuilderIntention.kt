package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.DslBuilderGenerator
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

class DslBuilderIntention : IntentionAction {
    override fun getText(): String = "Generate DSL builder"

    override fun getFamilyName(): String = "Kotlin Toolbox"

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ): Boolean {
        if (editor == null || file !is KtFile) return false
        if (DumbService.isDumb(project)) return false
        return getDataClass(editor, file) != null
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        if (editor == null || file !is KtFile) return
        val ktClass = getDataClass(editor, file) ?: return
        DslBuilderGenerator.generate(project, ktClass)
    }

    private fun getDataClass(
        editor: Editor,
        file: PsiFile,
    ): KtClass? {
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        return PsiTreeUtil
            .getParentOfType(element, KtClass::class.java)
            ?.takeIf { it.isData() }
    }
}
