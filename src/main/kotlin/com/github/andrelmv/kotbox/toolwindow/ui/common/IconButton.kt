package com.github.andrelmv.kotbox.toolwindow.ui.common

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.JBUI
import javax.swing.Icon

class IconButton(
    icon: Icon,
    tooltip: String,
    private val onClick: () -> Unit,
) {
    private val action =
        object : DumbAwareAction(tooltip, null, icon) {
            override fun actionPerformed(e: AnActionEvent) {
                onClick()
            }
        }

    val component: ActionButton =
        ActionButton(
            action,
            action.templatePresentation.clone(),
            ActionPlaces.TOOLBAR,
            JBUI.size(22),
        )
}
