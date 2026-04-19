package com.github.andrelmv.kotbox.inlay

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
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.awt.datatransfer.StringSelection

class CopyInterpolatedValueIntention : IntentionAction {
    override fun getText() = "Copy interpolated value"

    override fun getFamilyName() = "Kotlin Toolbox"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ): Boolean {
        if (editor == null || file !is KtFile) return false
        val offset = editor.caretModel.offset
        val property = file.findElementAt(offset)?.parentOfType<KtProperty>() ?: return false
        val initializer = property.initializer ?: return false
        if (initializer !is KtStringTemplateExpression || !initializer.hasInterpolation()) return false
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

// allowAnalysisOnEdt is required: isAvailable can be called on EDT (e.g. from popup selection path)
@OptIn(KaAllowAnalysisOnEdt::class)
private fun KtExpression.evaluateToString(): String? =
    allowAnalysisOnEdt {
        analyze(this@evaluateToString) {
            if (expressionType != builtinTypes.string) return@analyze null
            evaluate()?.toString()?.removeSurrounding("\"")
        }
    }
