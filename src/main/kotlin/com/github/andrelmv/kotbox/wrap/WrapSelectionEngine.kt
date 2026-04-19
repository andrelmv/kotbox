package com.github.andrelmv.kotbox.wrap

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Transformation engine that applies a [WrapperDescriptor] to the text
 * currently selected in the editor.
 *
 * Responsibilities:
 *  - Extract the selected text while preserving relative indentation
 *  - Replace the selection with the wrapped code
 *  - Add the required import via KtPsiFactory (compatible with K1 and K2)
 *  - Register the operation as a single command (single undo step)
 */
internal object WrapSelectionEngine {
    /**
     * Checks whether wrapping can be applied in the current context.
     * Called by both the IntentionAction and the AnAction to control
     * visibility and availability.
     */
    fun isAvailable(
        editor: Editor,
        file: PsiFile,
    ): Boolean = file is KtFile && editor.selectionModel.hasSelection()

    /**
     * Applies the wrap. Must be called on the EDT; the write action is managed
     * internally via [WriteCommandAction].
     *
     * @param project    current project
     * @param editor     editor with an active selection
     * @param file       current Kotlin file (already verified as KtFile)
     * @param descriptor wrapper to apply
     */
    fun wrap(
        project: Project,
        editor: Editor,
        file: KtFile,
        descriptor: WrapperDescriptor,
    ) {
        val selectionModel = editor.selectionModel
        if (!selectionModel.hasSelection()) return

        val selectedText = selectionModel.selectedText ?: return
        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd

        // Detect outer indentation (whitespace before the selection start on its line)
        val lineNumber = editor.document.getLineNumber(selectionStart)
        val lineStart = editor.document.getLineStartOffset(lineNumber)
        val outerIndent =
            editor.document.charsSequence
                .subSequence(lineStart, selectionStart)
                .takeWhile { it.isWhitespace() }
                .toString()

        // Build the body with correct indentation:
        // - First line:  outerIndent + "    " + text (no leading spaces from selection)
        // - Other lines: "    " + text (already carry the file's indentation)
        val indentedBody =
            selectedText
                .trimEnd()
                .lines()
                .mapIndexed { index, line ->
                    if (index == 0) "$outerIndent    $line" else "    $line"
                }.joinToString("\n")

        val rawWrapped = descriptor.wrapTemplate.format(indentedBody)

        // Align the closing brace with the outer indentation
        val wrappedText =
            rawWrapped
                .lines()
                .mapIndexed { index, line ->
                    if (index > 0 && line.trim() == "}") "$outerIndent}" else line
                }.joinToString("\n")

        WriteCommandAction.runWriteCommandAction(
            project,
            descriptor.displayName, // command name shown in the undo/redo history
            null,
            {
                // 1. Replace the selection
                editor.document.replaceString(selectionStart, selectionEnd, wrappedText)

                // 2. Sync PSI with the modified document
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)

                // 3. Add the imports if needed
                descriptor.requiredImports.forEach { addImportIfNeeded(project, file, it) }
            },
        )
    }

    /**
     * Inserts the import directly into the PSI via KtPsiFactory.
     * Compatible with Kotlin plugin K1 and K2.
     */
    private fun addImportIfNeeded(
        project: Project,
        file: KtFile,
        fqName: String,
    ) {
        val importList = file.importList ?: return
        val factory = KtPsiFactory(project)

        // Check if the import already exists
        val alreadyImported =
            importList.imports.any {
                it.importedFqName?.asString() == fqName
            }
        if (alreadyImported) return

        // Create and insert the import (avoids deprecated ImportPath API)
        val importDirective =
            factory.createFile("import $fqName\n").importDirectives.firstOrNull() ?: return
        importList.add(factory.createNewLine())
        importList.add(importDirective)
    }
}
