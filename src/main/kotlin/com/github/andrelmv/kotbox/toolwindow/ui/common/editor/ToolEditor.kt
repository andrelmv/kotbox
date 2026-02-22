package com.github.andrelmv.kotbox.toolwindow.ui.common.editor

import com.github.andrelmv.kotbox.toolwindow.ui.common.IconButton
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

class ToolEditor(
    private val project: Project? = null,
    private val language: EditorLanguage = EditorLanguage.PLAIN_TEXT,
    private val preferredLines: Int = 6,
    private val showToolbar: Boolean = true,
    private val softWraps: Boolean = true,
    private val parentDisposable: Disposable,
) {
    private val editor: EditorTextField = createEditorTextField()

    var text: String
        get() = editor.text
        set(value) {
            ApplicationManager.getApplication().runWriteAction {
                editor.document.setText(value)
            }
        }

    val component: JPanel by lazy { createComponent() }

    val document get() = editor.document

    private fun createEditorTextField(): EditorTextField {
        val lang = language.getLanguage()

        val textField =
            if (project != null) {
                LanguageTextField(lang, project, "", false)
            } else {
                EditorTextField()
            }

        return textField.apply {
            setOneLineMode(false)
            preferredSize = Dimension(0, preferredLines * 20)
            addSettingsProvider { editorEx -> configureEditor(editorEx) }
        }
    }

    private fun configureEditor(editorEx: EditorEx) {
        editorEx.settings.apply {
            isLineNumbersShown = true
            isLineMarkerAreaShown = true
            isFoldingOutlineShown = false
            isIndentGuidesShown = true
            isUseSoftWraps = softWraps
            isCaretRowShown = true // Highlight current line
            additionalLinesCount = 0
        }
        editorEx.setHorizontalScrollbarVisible(true)
        editorEx.setVerticalScrollbarVisible(true)
    }

    private fun createComponent(): JPanel {
        return if (showToolbar) {
            JPanel(BorderLayout()).apply {
                add(editor, BorderLayout.CENTER)
                add(createToolbar(), BorderLayout.EAST)
            }
        } else {
            JPanel(BorderLayout()).apply {
                add(editor, BorderLayout.CENTER)
            }
        }
    }

    private fun createToolbar(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 6)

            val buttons =
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    alignmentX = Component.CENTER_ALIGNMENT

                    // Copy button centered
                    add(createCenteredButton(IconButton(AllIcons.Actions.Copy, "Copy") { copyToClipboard() }.component))
                    add(Box.createVerticalStrut(8))

                    // Clear button centered
                    add(createCenteredButton(IconButton(AllIcons.Actions.GC, "Clear") { clear() }.component))
                    add(Box.createVerticalStrut(10))

                    // Centered separator
                    add(
                        JPanel(BorderLayout()).apply {
                            maximumSize = Dimension(Int.MAX_VALUE, 2)
                            add(JSeparator(SwingConstants.HORIZONTAL), BorderLayout.CENTER)
                        },
                    )
                }
            add(buttons, BorderLayout.NORTH)
        }
    }

    private fun createCenteredButton(button: JComponent): JPanel {
        return JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            isOpaque = false
            add(button)
        }
    }

    fun onTextChanged(listener: () -> Unit) {
        editor.document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    listener()
                }
            },
            parentDisposable,
        )
    }

    fun copyToClipboard() {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    fun clear() {
        text = ""
    }
}
