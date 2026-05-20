package com.github.andrelmv.kotbox.proto.dialog

import com.intellij.lang.Language
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent

class PreviewDialog(
    project: Project,
    private val protoContent: String,
) : DialogWrapper(project, true) {
    private val editorComponent by lazy { ProtoEditorProvider.create(project, protoContent) }

    init {
        title = "Proto Preview"
        init()
    }

    override fun createCenterPanel(): JComponent = editorComponent

    override fun createActions(): Array<Action> =
        arrayOf(
            object : DialogWrapperAction("Copy to clipboard") {
                override fun doAction(e: ActionEvent) {
                    CopyPasteManager.getInstance().setContents(StringSelection(protoContent))
                    close(OK_EXIT_CODE)
                }
            },
        )

    override fun dispose() {
        if (!isProtoPluginAvailable()) {
            EditorFactory
                .getInstance()
                .getEditors(EditorFactory.getInstance().createDocument(protoContent))
                .forEach { EditorFactory.getInstance().releaseEditor(it) }
        }
        super.dispose()
    }

    private fun isProtoPluginAvailable(): Boolean = Language.findLanguageByID(PROTOBUF) != null
}
