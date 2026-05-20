package com.github.andrelmv.kotbox.proto.dialog

import com.intellij.lang.Language
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import javax.swing.JComponent
import kotlin.apply

object ProtoEditorProvider {
    fun create(
        project: Project,
        protoContent: String,
    ): JComponent =
        if (isProtoPluginAvailable()) {
            createLanguageTextField(project, protoContent)
        } else {
            createFallbackViewer(project, protoContent)
        }

    private fun isProtoPluginAvailable(): Boolean = Language.findLanguageByID(PROTOBUF) != null

    private fun createLanguageTextField(
        project: Project,
        protoContent: String,
    ): JComponent {
        val textField =
            LanguageTextField(
                Language.findLanguageByID(PROTOBUF),
                project,
                protoContent,
                object : LanguageTextField.SimpleDocumentCreator() {},
                false,
            )
        return JBScrollPane(textField).apply {
            preferredSize = Dimension(650, 450)
        }
    }

    private fun createFallbackViewer(
        project: Project,
        protoContent: String,
    ): JComponent {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument(protoContent)
        val editor =
            editorFactory.createViewer(document, project).apply {
                settings.apply {
                    isLineNumbersShown = true
                    isFoldingOutlineShown = false
                    isLineMarkerAreaShown = false
                    isWhitespacesShown = false
                    isUseSoftWraps = false
                    additionalLinesCount = 0
                }
            }

        return editor.scrollingModel.let {
            (editor as EditorEx).scrollPane.apply {
                preferredSize = Dimension(650, 450)
            }
        }
    }
}

internal const val PROTOBUF = "protobuf"
