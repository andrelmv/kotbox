package com.github.andrelmv.kotbox.toolwindow.ui.window

import com.github.andrelmv.kotbox.toolwindow.ui.tool.base.DeveloperTool
import com.github.andrelmv.kotbox.toolwindow.ui.tool.jwt.JwtEncoderDecoder
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JSeparator

class MainWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val tools = createTools(project)

        tools.forEach { tool ->
            Disposer.register(toolWindow.disposable, tool)
        }

        val cardLayout = CardLayout()
        val cardPanel = JPanel(cardLayout)

        tools.forEach { tool ->
            cardPanel.add(tool.createComponent(), tool.displayName)
        }

        val currentToolLabel =
            JBLabel(tools.first().displayName).apply {
                font = font.deriveFont(Font.BOLD)
            }

        var currentTool = tools.first()
        currentTool.activated()

        val menuButton =
            createToolMenuButton(
                icon = MENU_ICON,
                tools = tools,
                onToolSelected = { selectedTool ->
                    currentTool.deactivated()
                    cardLayout.show(cardPanel, selectedTool.displayName)
                    currentToolLabel.text = selectedTool.displayName
                    currentTool = selectedTool
                    currentTool.activated()
                },
            )

        val headerPanel = createHeaderPanel(menuButton, currentToolLabel)

        val rootPanel =
            JPanel(BorderLayout()).apply {
                add(headerPanel, BorderLayout.NORTH)
                add(cardPanel, BorderLayout.CENTER)
            }

        val content = ContentFactory.getInstance().createContent(rootPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    private fun createTools(project: Project): List<DeveloperTool> {
        return listOf(
            JwtEncoderDecoder(project),
        )
    }

    private fun createHeaderPanel(
        menuButton: ActionButton,
        toolLabel: JBLabel,
    ): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(
                JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)).apply {
                    add(menuButton)
                    add(toolLabel)
                },
            )
            add(JSeparator())
        }
    }

    private fun createToolMenuButton(
        icon: Icon,
        tools: List<DeveloperTool>,
        onToolSelected: (DeveloperTool) -> Unit,
    ): ActionButton {
        val action =
            object : DumbAwareAction("Show Tools", null, icon) {
                override fun actionPerformed(e: AnActionEvent) {
                    val source = e.inputEvent?.component ?: return

                    val group =
                        DefaultActionGroup().apply {
                            tools.forEach { tool ->
                                add(
                                    object : AnAction(tool.displayName) {
                                        override fun actionPerformed(e: AnActionEvent) {
                                            onToolSelected(tool)
                                        }
                                    },
                                )
                            }
                        }

                    JBPopupFactory.getInstance()
                        .createActionGroupPopup(
                            null,
                            group,
                            DataManager.getInstance().getDataContext(source),
                            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                            false,
                        )
                        .showUnderneathOf(source)
                }
            }

        return ActionButton(
            action,
            action.templatePresentation.clone(),
            ActionPlaces.TOOLBAR,
            JBUI.size(22),
        )
    }
}

private val MENU_ICON by lazy {
    IconLoader.getIcon("/icons/menu.svg", MainWindowFactory::class.java.classLoader)
}
