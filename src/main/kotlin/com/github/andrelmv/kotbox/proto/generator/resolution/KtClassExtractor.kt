package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import com.github.andrelmv.kotbox.utils.isDataClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeReference

internal object KtClassExtractor {
    /**
     * Finds the [KtClass] this field references.
     * Returns non-null only when [typeMapping] indicates a user-defined class is involved.
     */
    @OptIn(KaExperimentalApi::class)
    fun KaSession.resolveClass(
        typeReference: KtTypeReference,
        typeMapping: ProtoTypeMapping?,
    ): KtClass? =
        when (typeMapping) {
            is ProtoTypeMapping.CollectionTypeMapping if typeMapping.customElement ->
                typeReference
                    .typeArgument(0)
                    ?.let { findCustomClass(it) }
                    ?: findExpandedTypeArgument(typeReference, 0)
            is ProtoTypeMapping.MapTypeMapping if typeMapping.customValue ->
                typeReference
                    .typeArgument(1)
                    ?.let { findCustomClass(it) }
                    ?: findExpandedTypeArgument(typeReference, 1)
            null -> resolveKtClass(typeReference)?.takeIf { it.isDataClass() || it.isEnum() }
            else -> null
        }

    /** Resolves a [KtTypeReference] to its source [KtClass], or null for non-source types. */
    private fun KaSession.resolveKtClass(typeReference: KtTypeReference): KtClass? {
        // expandedClassType unwraps type aliases before we inspect the type
        val classType = expandedClassType(typeReference) ?: return null
        val classSymbol = classType.symbol as? KaClassSymbol ?: return null
        return classSymbol.psi as? KtClass
    }

    /**
     * Extracts a type argument's [KtClass] directly from the K2 expanded type.
     * Used when [typeArgument] returns null because the PSI is a type alias with no angle-bracket children
     * (e.g. `typealias AddressList = List<Address>`).
     */
    private fun KaSession.findExpandedTypeArgument(
        typeReference: KtTypeReference,
        index: Int,
    ): KtClass? {
        val expandedType = expandedClassType(typeReference) ?: return null
        val argType = expandedTypeArgument(expandedType, index) ?: return null
        val psi = (argType.symbol as? KaClassSymbol)?.psi as? KtClass
        return psi?.takeIf { it.isDataClass() || it.isEnum() }
    }

    private fun KaSession.findCustomClass(typeReference: KtTypeReference): KtClass? =
        resolveKtClass(typeReference)?.takeIf { it.isDataClass() || it.isEnum() }

    private fun KtTypeReference.typeArgument(index: Int): KtTypeReference? =
        typeElement
            ?.typeArgumentsAsTypes
            ?.getOrNull(index)
}
