package com.github.andrelmv.kotbox.wrap

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

/**
 * IntentionAction that appears in the Alt+Enter / Option+Space menu when
 * text is selected in a Kotlin file.
 *
 * Shows a popup listing all wrappers registered in [CoroutineWrapperRegistry]
 * and applies the chosen one via [WrapSelectionEngine].
 *
 * Implements [LowPriorityAction] to appear below high-priority suggestions
 * from the official Kotlin plugin.
 */
class WrapWithCoroutineIntention :
    IntentionAction,
    LowPriorityAction {
    override fun getFamilyName(): String = "Kotlin Toolbox"

    override fun getText(): String = "Wrap with coroutine builder..."

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile,
    ): Boolean {
        if (editor == null) return false
        return WrapSelectionEngine.isAvailable(editor, file)
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile,
    ) {
        if (editor == null) return
        val ktFile = file as? KtFile ?: return

        val wrappers = CoroutineWrapperRegistry.all

        // Popup with the list of available wrappers
        val step =
            object : BaseListPopupStep<WrapperDescriptor>(
                "Kotlin Toolbox — wrap selection",
                wrappers,
            ) {
                override fun getTextFor(value: WrapperDescriptor): String = value.displayName

                override fun onChosen(
                    selectedValue: WrapperDescriptor,
                    finalChoice: Boolean,
                ): PopupStep<*>? {
                    if (finalChoice) {
                        WrapSelectionEngine.wrap(project, editor, ktFile, selectedValue)
                    }
                    return PopupStep.FINAL_CHOICE
                }

                override fun isSpeedSearchEnabled(): Boolean = true
            }

        JBPopupFactory
            .getInstance()
            .createListPopup(step)
            .showInBestPositionFor(editor)
    }

    // Write action is managed internally by WrapSelectionEngine
    override fun startInWriteAction(): Boolean = false
}
