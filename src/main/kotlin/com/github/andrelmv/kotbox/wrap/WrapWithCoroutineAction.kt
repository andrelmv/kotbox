package com.github.andrelmv.kotbox.wrap

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.jetbrains.kotlin.psi.KtFile

/**
 * AnAction that applies a specific wrapper to the selected code.
 *
 * One instance of this class is registered per wrapper in plugin.xml
 * via a concrete subclass in the wrappers package.
 *
 * Each action appears individually in Settings → Keymap, allowing
 * the user to configure independent shortcuts for each wrapper.
 *
 * @param wrapperId ID of the wrapper to apply, as defined in [CoroutineWrapperRegistry]
 */
open class WrapWithCoroutineAction(
    private val wrapperId: String,
) : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT // runs on a background thread, not on the EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val descriptor = CoroutineWrapperRegistry.findById(wrapperId)

        e.presentation.isEnabledAndVisible =
            editor != null &&
            file != null &&
            descriptor != null &&
            WrapSelectionEngine.isAvailable(editor, file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        val descriptor = CoroutineWrapperRegistry.findById(wrapperId) ?: return

        WrapSelectionEngine.wrap(project, editor, file, descriptor)
    }
}
