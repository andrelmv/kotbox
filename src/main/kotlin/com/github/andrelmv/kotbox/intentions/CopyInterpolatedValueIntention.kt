package com.github.andrelmv.kotbox.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.datatransfer.StringSelection

class CopyInterpolatedValueIntention : IntentionAction {
    override fun getText() = "Copy string value"

    override fun getFamilyName() = "Kotlin intentions"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ): Boolean {
        if (editor == null || file !is KtFile) return false
        val offset = editor.caretModel.offset
        val property = file.findElementAt(offset)?.parentOfType<KtProperty>() ?: return false
        val initializer = property.initializer ?: return false
        return initializer.evaluateToString() != null
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        if (editor == null || file !is KtFile) return

        val offset = editor.caretModel.offset
        val property = file.findElementAt(offset)?.parentOfType<KtProperty>() ?: return
        val valueExpr: KtExpression = property.initializer ?: return

        val evaluated = valueExpr.evaluateToString() ?: return

        CopyPasteManager.getInstance().setContents(StringSelection(evaluated))
    }

    override fun startInWriteAction() = false
}

@OptIn(KaAllowAnalysisOnEdt::class)
private fun KtExpression.evaluateToString(): String? =
    allowAnalysisOnEdt {
        analyze(this@evaluateToString) {
            if (expressionType != builtinTypes.string) return@analyze null
            evaluate()?.toString()?.removeSurrounding("\"")
        }
    }
