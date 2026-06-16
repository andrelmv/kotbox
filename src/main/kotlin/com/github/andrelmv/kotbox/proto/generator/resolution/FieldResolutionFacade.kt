package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import com.github.andrelmv.kotbox.proto.generator.resolution.KotlinToProtoMapper.resolveExpanded
import com.github.andrelmv.kotbox.proto.generator.resolution.KtClassExtractor.resolveClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Resolves a constructor-parameter [KtTypeReference] into a [ClassGraph.FieldResolution], sharing a single K2 analysis session.
 *
 * The mapping and the class resolution each require expanding the type via K2. Doing them
 * in one [analyze] block avoids re-opening a session and re-expanding the type twice.
 */
internal object FieldResolutionFacade {
    @OptIn(KaExperimentalApi::class)
    fun resolve(typeReference: KtTypeReference): ClassGraph.FieldResolution {
        val protoTypeMapping = KotlinToProtoMapper.resolveFromText(typeReference.text)

        if (protoTypeMapping != null && !protoTypeMapping.needsClassResolution()) {
            return ClassGraph.FieldResolution(
                protoTypeMapping = protoTypeMapping,
                resolved = null,
            )
        }

        // one shared session yields both the mapping (alias fallback) and the referenced class
        return analyze(typeReference) {
            val mapping = protoTypeMapping ?: this.resolveExpanded(typeReference)
            val resolved =
                this.resolveClass(
                    typeReference = typeReference,
                    typeMapping = mapping,
                )

            ClassGraph.FieldResolution(
                protoTypeMapping = mapping,
                resolved = resolved,
            )
        }
    }
}

/**
 * Returns true when a [ProtoTypeMapping] involves a user-defined class that must be resolved from source.
 */
private fun ProtoTypeMapping?.needsClassResolution(): Boolean =
    when (this) {
        is ProtoTypeMapping.ScalarTypeMapping -> false
        is ProtoTypeMapping.CollectionTypeMapping -> customElement
        is ProtoTypeMapping.MapTypeMapping -> customValue
        null -> true
    }
