package com.github.andrelmv.kotbox.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.awt.Toolkit.getDefaultToolkit
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
        val element = file.findElementAt(offset) ?: return false
        return element.parentOfType<KtProperty>()?.initializer is KtStringTemplateExpression
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        if (editor == null || file !is KtFile) return

        val offset = editor.caretModel.offset
        val element: PsiElement = file.findElementAt(offset) ?: return
        val property: KtProperty = element.parentOfType<KtProperty>() ?: return
        val valueExpr: KtExpression = property.initializer ?: return

        val evaluated = evaluateExpression(expression = valueExpr)

        val clipboard = StringSelection(evaluated)
        getDefaultToolkit().systemClipboard.setContents(clipboard, clipboard)
    }

    override fun startInWriteAction() = false

    private fun evaluateExpression(expression: KtExpression): String =
        when (expression) {
            is KtStringTemplateExpression ->
                expression.entries.joinToString("") { entry ->
                    when {
                        entry is KtLiteralStringTemplateEntry -> entry.text
                        entry is KtEscapeStringTemplateEntry -> entry.unescapedValue
                        entry is KtSimpleNameStringTemplateEntry -> {
                            val resolved = (entry.expression as? KtNameReferenceExpression)?.mainReference?.resolve()
                            val initializer = (resolved as? KtProperty)?.initializer
                            if (initializer != null) evaluateExpression(initializer) else ""
                        }

                        else -> ""
                    }
                }

            else -> expression.text // fallback
        }
}
