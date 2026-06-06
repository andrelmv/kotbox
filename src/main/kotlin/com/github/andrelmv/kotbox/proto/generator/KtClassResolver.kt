package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.utils.isDataClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeReference

object KtClassResolver {
    /**
     * Finds the [KtClass] this field references, for use by the graph walker.
     * Returns non-null only when [mapped] indicates a user-defined class is involved.
     *
     * For type aliases like `typealias AddressList = List<Address>`, the type arguments
     * aren't visible in the PSI, so falls back to K2 type expansion to find them.
     */
    @OptIn(KaExperimentalApi::class)
    fun findReferencedClass(
        typeReference: KtTypeReference,
        mapped: MappedType?,
    ): KtClass? =
        when (mapped) {
            is MappedType.CollectionType if mapped.customElement ->
                typeReference
                    .typeArgument(0)
                    ?.let { findDataClass(it) }
                    ?: findExpandedTypeArgument(typeReference, 0)
            is MappedType.MapType if mapped.customValue ->
                typeReference
                    .typeArgument(1)
                    ?.let { findDataClass(it) }
                    ?: findExpandedTypeArgument(typeReference, 1)
            null -> findDataClass(typeReference) ?: findEnumClass(typeReference)
            else -> null
        }

    /** Resolves a [KtTypeReference] to its source [KtClass], or null for non-source types. */
    private fun resolveKtClass(typeReference: KtTypeReference): KtClass? =
        analyze(typeReference) {
            // fullyExpandedType unwraps type aliases before we inspect the type
            val classType = typeReference.type.fullyExpandedType as? KaClassType ?: return@analyze null
            val classSymbol = classType.symbol as? KaClassSymbol ?: return@analyze null
            classSymbol.psi as? KtClass
        }

    /**
     * Extracts a type argument's [KtClass] directly from the K2 expanded type.
     * Used when [typeArgument] returns null because the PSI is a type alias with no angle-bracket children
     * (e.g. `typealias AddressList = List<Address>`).
     */
    @OptIn(KaExperimentalApi::class)
    private fun findExpandedTypeArgument(
        typeReference: KtTypeReference,
        index: Int,
    ): KtClass? =
        analyze(typeReference) {
            val expandedType = typeReference.type.fullyExpandedType as? KaClassType ?: return@analyze null
            val argType =
                expandedType.typeArguments
                    .getOrNull(index)
                    ?.type
                    ?.fullyExpandedType as? KaClassType ?: return@analyze null
            val psi = (argType.symbol as? KaClassSymbol)?.psi as? KtClass
            psi?.takeIf { it.isDataClass() }
        }

    private fun findDataClass(typeReference: KtTypeReference): KtClass? = resolveKtClass(typeReference)?.takeIf { it.isDataClass() }

    private fun findEnumClass(typeReference: KtTypeReference): KtClass? = resolveKtClass(typeReference)?.takeIf { it.isEnum() }

    private fun KtTypeReference.typeArgument(index: Int): KtTypeReference? = typeElement?.typeArgumentsAsTypes?.getOrNull(index)
}
