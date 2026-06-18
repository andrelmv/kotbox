package com.github.andrelmv.kotbox.proto.generator.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Expands [typeReference] to a [KaClassType], unwrapping any type aliases.
 */
@OptIn(KaExperimentalApi::class)
internal fun KaSession.expandedClassType(typeReference: KtTypeReference): KaClassType? =
    typeReference.type.fullyExpandedType as? KaClassType

/**
 * Expands the type argument at [index] of [classType], unwrapping any type aliases.
 */
@OptIn(KaExperimentalApi::class)
internal fun KaSession.expandedTypeArgument(
    classType: KaClassType,
    index: Int,
): KaClassType? =
    classType.typeArguments
        .getOrNull(index)
        ?.type
        ?.fullyExpandedType as? KaClassType

internal val KaClassType.shortName: String
    get() = classId.shortClassName.asString()
