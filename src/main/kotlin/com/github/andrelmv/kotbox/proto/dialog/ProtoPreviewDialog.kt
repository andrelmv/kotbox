package com.github.andrelmv.kotbox.proto.dialog

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.UIManager

class ProtoPreviewDialog(
    project: Project,
    private val protoContent: String,
) : DialogWrapper(project, true) {
    private val textArea by lazy {
        JTextArea(protoContent).apply {
            isEditable = false
            font = Font("JetBrains Mono", Font.PLAIN, 13)
            margin = JBUI.insets(8)
            background = UIManager.getColor("Editor.background")
            foreground = UIManager.getColor("Editor.foreground")
        }
    }

    init {
        title = "Proto Preview"
        init()
    }

    override fun createActions(): Array<Action> =
        arrayOf(
            object : DialogWrapperAction("Copy to clipboard") {
                override fun doAction(e: ActionEvent) {
                    CopyPasteManager.getInstance().setContents(StringSelection(protoContent))
                    close(OK_EXIT_CODE)
                }
            },
        )

    override fun createCenterPanel(): JComponent =
        JBScrollPane(textArea).apply {
            preferredSize = Dimension(600, 400)
        }
}
