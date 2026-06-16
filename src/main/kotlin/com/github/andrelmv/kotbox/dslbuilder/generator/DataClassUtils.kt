package com.github.andrelmv.kotbox.dslbuilder.generator

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Result of K2 analysis for a constructor parameter: nullability and default-value
 * presence resolved in a single analyze {} session for efficiency.
 */
data class DslBuilderParamAnalysis(
    val isNullable: Boolean,
    val hasDefault: Boolean,
)

internal fun KtParameter.analyzeK2(): DslBuilderParamAnalysis {
    // PSI-syntactic fallbacks — no resolve required, work in all contexts including tests.
    // Covers the common cases: explicit "?" marker and "= expr" default.
    val isNullablePsi = typeReference?.text?.trim()?.endsWith("?") ?: false
    val hasDefaultPsi = hasDefaultValue()

    return try {
        analyze(this) {
            val sym = symbol as? KaValueParameterSymbol
            DslBuilderParamAnalysis(
                isNullable = sym?.returnType?.isMarkedNullable ?: isNullablePsi,
                hasDefault = sym?.hasDefaultValue ?: hasDefaultPsi,
            )
        }
    } catch (_: Exception) {
        DslBuilderParamAnalysis(isNullable = isNullablePsi, hasDefault = hasDefaultPsi)
    }
}

/**
 * Returns the simple (unqualified) type name of the parameter, stripped of generic
 * arguments and the nullable marker. Suitable only for stub-index lookups by short name.
 * Do NOT use to detect List/Set/Map — use typeReference.text for that.
 *
 * Examples: "List<String>?" → "List"  |  "Address?" → "Address"
 */
internal fun KtParameter.simpleTypeName(): String? {
    val typeText = typeReference?.text ?: return null
    return typeText
        .substringBefore("<") // remove type args
        .removeSuffix("?") // remove nullable marker
        .trim()
}
