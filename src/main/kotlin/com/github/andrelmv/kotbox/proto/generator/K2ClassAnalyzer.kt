package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.proto.generator.rules.CollectionResolutionRule
import com.github.andrelmv.kotbox.proto.generator.rules.FallbackResolutionRule
import com.github.andrelmv.kotbox.proto.generator.rules.FieldResolutionRule
import com.github.andrelmv.kotbox.proto.generator.rules.MapResolutionRule
import com.github.andrelmv.kotbox.proto.generator.rules.ScalarResolutionRule
import com.github.andrelmv.kotbox.utils.isDataClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class K2ClassAnalyzer(
    private val rules: List<FieldResolutionRule> = defaultRules(),
) {
    fun analyze(rootClass: KtClass): ProtoMessage {
        require(rootClass.isDataClass()) { "'${rootClass.name}' is not a data class" }

        val graph = ClassGraphBuilder.build(rootClass)

        // Process in reverse, so dependencies are protoMessages before dependents
        val protoMessages = mutableMapOf<String, ProtoMessage>()
        graph.classes.entries
            .reversed()
            .forEach { (fqn, resolvedClass) ->
                protoMessages[fqn] =
                    buildMessage(
                        resolvedClass = resolvedClass,
                        protoMessages = protoMessages,
                    )
            }

        return protoMessages.getValue(rootClass.fullyQualifiedName)
    }

    /**
     * Converts a [ClassGraph.ResolvedClass] into a [ProtoMessage] for each constructor parameter.
     *
     * [protoMessages] holds already-built dependency messages so nested fields can reference them by FQN
     * without rebuilding.
     */
    private fun buildMessage(
        resolvedClass: ClassGraph.ResolvedClass,
        protoMessages: Map<String, ProtoMessage>,
    ): ProtoMessage {
        val ktClass = resolvedClass.ktClass
        val fields =
            ktClass
                .primaryConstructorParameters
                .filter { it.name != null && it.typeReference != null }
                .mapIndexed { index, param ->
                    val resolution = resolvedClass.fieldResolutions.getValue(param.name!!)
                    val nestedMessage =
                        resolution.resolved
                            ?.takeIf { it.isDataClass() }
                            ?.let { protoMessages[it.fullyQualifiedName] }
                    val nestedEnum = resolution.resolved?.takeIf { it.isEnum() }

                    rules.firstNotNullOf {
                        it.tryExecute(
                            name = param.name!!,
                            typeText = param.typeReference!!.text,
                            number = index + 1,
                            mappedType = resolution.mapped,
                            nestedMessage = nestedMessage,
                            nestedEnum = nestedEnum?.toProtoEnumModel(),
                        )
                    }
                }

        return ProtoMessage(
            name = ktClass.name!!,
            fields = fields,
        )
    }

    companion object {
        fun defaultRules() =
            listOf(
                CollectionResolutionRule(),
                MapResolutionRule(),
                ScalarResolutionRule(),
                FallbackResolutionRule(),
            )
    }
}

internal fun KtClass.toProtoEnumModel(): ProtoEnumModel {
    val entries =
        declarations
            .filterIsInstance<KtEnumEntry>()
            .mapNotNull { it.name }
            .let { LinkedHashSet(it) }
    return ProtoEnumModel(name = name!!, entries = entries)
}
