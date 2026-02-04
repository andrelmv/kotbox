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
        runReadAction {
            element.accept(
                object : KotlinRecursiveElementVisitor() {
                    override fun visitBinaryExpression(expression: KtBinaryExpression) {
                        if (expression.operationToken == org.jetbrains.kotlin.lexer.KtTokens.PLUS) {
                            val parent = expression.parent

                            if (parent !is KtBinaryExpression || parent.operationToken != org.jetbrains.kotlin.lexer.KtTokens.PLUS) {
                                val stringTemplates = mutableListOf<KtStringTemplateExpression>()
                                collectStringTemplates(expression, stringTemplates)

                                stringTemplates.forEach { stringTemplate ->
                                    if (settings.state.withStringInterpolationHint && stringTemplate.isKtStringTemplateExpression()) {
                                        stringTemplate.getValue()
                                            ?.let {
                                                val offset = stringTemplate.lastChild.textOffset
                                                val base = factory.text(it)
                                                val inlayPresentation = factory.roundWithBackground(base)
                                                sink.addInlineElement(offset, true, inlayPresentation, true)
                                            }
                                    }
                                }
                            }
                        }
                        super.visitBinaryExpression(expression)
                    }

                    override fun visitElement(element: PsiElement) {
                        if (settings.state.withStringInterpolationHint && element.isKtStringTemplateExpression()) {
                            val parent = element.parent
                            if (parent !is KtBinaryExpression || parent.operationToken != org.jetbrains.kotlin.lexer.KtTokens.PLUS) {
                                (element as KtStringTemplateExpression).getValue()
                                    ?.let {
                                        val offset = element.lastChild.textOffset
                                        val base = factory.text(it)
                                        val inlayPresentation = factory.roundWithBackground(base)
                                        sink.addInlineElement(offset, true, inlayPresentation, true)
                                    }
                            }
                            return
                        } else if (settings.state.withStringConstantHint && element.isKtNameReferenceExpression()) {
                            (element as KtNameReferenceExpression).getValue()
                                ?.let {
                                    val offset = element.getReferencedNameElement().text.length + element.textOffset
                                    val base = factory.text(it)
                                    val inlayPresentation = factory.roundWithBackground(base)
                                    sink.addInlineElement(offset, true, inlayPresentation, false)
                                }
                        }
                    }
                },
            )
        }
        return true
    }
}

private fun collectStringTemplates(
    expr: KtExpression,
    result: MutableList<KtStringTemplateExpression>,
) {
    when (expr) {
        is KtBinaryExpression -> {
            expr.left
                ?.let { collectStringTemplates(it, result) }
            expr.right
                ?.let { collectStringTemplates(it, result) }
        }

        is KtStringTemplateExpression -> result.add(expr)
    }
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
