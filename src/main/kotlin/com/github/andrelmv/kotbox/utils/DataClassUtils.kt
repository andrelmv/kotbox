package com.github.andrelmv.kotbox.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass

/**
 * Returns true if this class is a data class.
 * Uses ktClass.isData() which is a syntactic PSI check — no resolve needed,
 * safe to call on source-declared classes without K2.
 */
internal fun KtClass.isDataClass(): Boolean = this.isData()

internal fun getDataClass(e: AnActionEvent): KtClass? {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    val file = e.getData(CommonDataKeys.PSI_FILE) ?: return null
    val element = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiTreeUtil
        .getParentOfType(element, KtClass::class.java)
        ?.takeIf { it.isData() }
}
