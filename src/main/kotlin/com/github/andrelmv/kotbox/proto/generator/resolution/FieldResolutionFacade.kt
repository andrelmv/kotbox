package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import com.github.andrelmv.kotbox.proto.generator.resolution.KotlinToProtoMapper.resolveExpanded
import com.github.andrelmv.kotbox.proto.generator.resolution.KtClassExtractor.resolveClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtTypeReference

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

    private fun ProtoTypeMapping?.needsClassResolution(): Boolean =
        when (this) {
            is ProtoTypeMapping.ScalarTypeMapping -> false
            is ProtoTypeMapping.CollectionTypeMapping -> customElement
            is ProtoTypeMapping.MapTypeMapping -> customValue
            null -> true
        }
}
