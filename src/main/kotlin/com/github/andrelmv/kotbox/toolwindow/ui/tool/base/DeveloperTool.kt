package com.github.andrelmv.kotbox.toolwindow.ui.tool.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

abstract class DeveloperTool(
    protected val project: Project,
) : Disposable {
    abstract val displayName: String

    protected open val wrapInScrollPane: Boolean = true

    fun createComponent(): JComponent {
        val contentPanel =
            panel {
                buildUi()
            }.apply {
                border = JBUI.Borders.empty(8)
            }

        afterBuildUi()

        return if (wrapInScrollPane) {
            JPanel(BorderLayout()).apply {
                add(
                    JBScrollPane(contentPanel).apply {
                        border = BorderFactory.createEmptyBorder()
                        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                    },
                    BorderLayout.CENTER,
                )
            }
        } else {
            contentPanel
        }
    }

    protected abstract fun Panel.buildUi()

    protected open fun afterBuildUi() {}

    open fun activated() {}

    open fun deactivated() {}

    open fun reset() {}

    override fun dispose() {}
}
