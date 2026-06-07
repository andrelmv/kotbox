package com.github.andrelmv.kotbox.proto.generator

import org.jetbrains.kotlin.psi.KtClass

/**
 * Builds a [ClassGraph] by performing a depth-first traversal of the class hierarchy
 * starting from a root data class.
 */
internal object ClassGraphBuilder {
    fun build(rootClass: KtClass): ClassGraph {
        val classes = mutableMapOf<String, ClassGraph.ResolvedClass>()

        val stack = ArrayDeque<KtClass>().apply { addLast(rootClass) }
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val fqn = current.fullyQualifiedName

            if (fqn in classes) continue

            val fieldResolutions =
                current.primaryConstructorParameters
                    .mapNotNull { param ->
                        val name = param.name ?: return@mapNotNull null
                        val typeReference = param.typeReference ?: return@mapNotNull null
                        val mapped: MappedType? = KotlinToProtoMapper.resolve(typeReference)
                        val resolvedKtClass: KtClass? =
                            KtClassResolver
                                .findReferencedClass(typeReference, mapped)
                                ?.also { stack.addLast(it) }

                        name to ClassGraph.FieldResolution(mapped, resolvedKtClass)
                    }.toMap()

            classes[fqn] = ClassGraph.ResolvedClass(current, fieldResolutions)
        }

        return ClassGraph(classes)
    }
}

internal data class ClassGraph(
    val classes: Map<String, ResolvedClass>, // fqn -> resolved class
) {
    data class ResolvedClass(
        val ktClass: KtClass,
        val fieldResolutions: Map<String, FieldResolution>, // paramName -> resolution
    )

    data class FieldResolution(
        val mapped: MappedType?,
        val resolved: KtClass?,
    )
}

internal val KtClass.fullyQualifiedName: String
    get() = fqName?.asString() ?: name!!
