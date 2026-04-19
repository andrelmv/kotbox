package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.DslBuilderGenerator
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbService
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass

class DslBuilderAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)

        val enabled =
            project != null &&
                editor != null &&
                file != null &&
                !DumbService.isDumb(project) &&
                getDataClass(e) != null

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val ktClass = getDataClass(e) ?: return
        DslBuilderGenerator.generate(project, ktClass)
    }

    private fun getDataClass(e: AnActionEvent): KtClass? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        return PsiTreeUtil
            .getParentOfType(element, KtClass::class.java)
            ?.takeIf { it.isData() }
    }
}
