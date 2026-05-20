package com.github.andrelmv.kotbox.proto

import com.github.andrelmv.kotbox.proto.generator.ProtoGenerator
import com.github.andrelmv.kotbox.utils.getDataClass
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbService

/**
 * Right-click → "Generate Proto Message" action.
 *
 * Visible and enabled only when the caret is inside a Kotlin data class.
 * Delegates immediately to [ProtoGenerator], which owns the orchestration
 * (background analysis, dialog, file placement).
 */
internal class ProtoAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val project = event.project ?: return
        val enabled = !DumbService.isDumb(project) && getDataClass(event) != null

        event.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val ktClass = getDataClass(event) ?: return
        ProtoGenerator.generate(project, ktClass)
    }
}
