package com.github.andrelmv.kotbox.inlay

import com.github.andrelmv.kotbox.inlay.config.InlayStringInterpolationSettings
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted

@Suppress("UnstableApiUsage")
class InlayStringInterpolationHintCollector(
    private val settings: InlayStringInterpolationSettings,
    editor: Editor,
) : FactoryInlayHintsCollector(editor) {
    override fun collect(
        element: PsiElement,
        editor: Editor,
        sink: InlayHintsSink,
    ): Boolean {
        runReadAction { element.accept(createVisitor(sink)) }
        return true
    }

    private fun createVisitor(sink: InlayHintsSink) =
        object : KotlinRecursiveElementVisitor() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (settings.state.withStringInterpolationHint && expression.isTopLevelPlusExpression()) {
                    collectStringTemplates(expression)
                        .filter { it.isKtStringTemplateExpression() }
                        .forEach { addStringTemplateHint(it, sink) }
                }
                super.visitBinaryExpression(expression)
            }

            override fun visitElement(element: PsiElement) {
                when {
                    settings.state.withStringInterpolationHint && element.isKtStringTemplateExpression() -> {
                        if (!element.isInPlusExpression()) {
                            addStringTemplateHint(element as KtStringTemplateExpression, sink)
                        }
                        return
                    }
                    settings.state.withStringConstantHint && element.isKtNameReferenceExpression() ->
                        addNameReferenceHint(element as KtNameReferenceExpression, sink)
                }
            }
        }

    private fun addStringTemplateHint(
        template: KtStringTemplateExpression,
        sink: InlayHintsSink,
    ) {
        template.getValue()?.let {
            val offset = template.lastChild.textOffset
            sink.addInlineElement(offset, true, factory.roundWithBackground(factory.text(it)), true)
        }
    }

    private fun addNameReferenceHint(
        reference: KtNameReferenceExpression,
        sink: InlayHintsSink,
    ) {
        reference.getValue()?.let {
            val offset = reference.getReferencedNameElement().text.length + reference.textOffset
            sink.addInlineElement(offset, true, factory.roundWithBackground(factory.text(it)), false)
        }
    }
}

private fun PsiElement.isInPlusExpression(): Boolean {
    val parent = parent
    return parent is KtBinaryExpression && parent.operationToken == KtTokens.PLUS
}

private fun KtBinaryExpression.isTopLevelPlusExpression(): Boolean = operationToken == KtTokens.PLUS && !isInPlusExpression()

private fun collectStringTemplates(expr: KtExpression): List<KtStringTemplateExpression> =
    when (expr) {
        is KtBinaryExpression -> {
            val left = expr.left?.let { collectStringTemplates(it) } ?: emptyList()
            val right = expr.right?.let { collectStringTemplates(it) } ?: emptyList()
            left + right
        }
        is KtStringTemplateExpression -> listOf(expr)
        else -> emptyList()
    }

private fun PsiElement.isKtNameReferenceExpression(): Boolean {
    return this is KtNameReferenceExpression &&
        (
            (this.context is KtCollectionLiteralExpression && this.isConstant()) ||
                (this.context is KtValueArgument && this.isConstant()) ||
                this.context is KtNamedFunction ||
                this.context is KtBinaryExpression
        )
}

private fun PsiElement.isKtStringTemplateExpression(): Boolean {
    if (this !is KtStringTemplateExpression) return false

    return runReadAction {
        this.isPlain().not() && this.hasInterpolation() && this.isSingleQuoted()
    } && this.isConstant()
}

@OptIn(KaAllowAnalysisOnEdt::class)
private fun KtExpression.isConstant(): Boolean {
    return allowAnalysisOnEdt {
        runReadAction {
            org.jetbrains.kotlin.analysis.api.analyze(this@isConstant) {
                evaluate() != null
            }
        }
    }
}

@OptIn(KaAllowAnalysisOnEdt::class)
private fun KtExpression.getValue(): String? {
    return allowAnalysisOnEdt {
        runReadAction {
            org.jetbrains.kotlin.analysis.api.analyze(this@getValue) {
                evaluate()?.toString()?.removeSurrounding("\"")
            }
        }
    }
}
