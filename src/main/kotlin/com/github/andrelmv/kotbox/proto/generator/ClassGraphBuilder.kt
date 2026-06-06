package com.github.andrelmv.kotbox.proto.generator

import org.jetbrains.kotlin.psi.KtClass

/**
 * Builds a [ClassGraph] by performing a breadth-first traversal of the class hierarchy
 * starting from a root data class.
 */
object ClassGraphBuilder {
    fun build(rootClass: KtClass): ClassGraph {
        val classes = mutableMapOf<String, ClassGraph.ResolvedClass>()

        val stack = ArrayDeque<KtClass>().apply { addLast(rootClass) }
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val fqn = current.fullyQualifiedName

            if (fqn in classes) continue

            val fieldResolutions =
                current.primaryConstructorParameters
                    .filter { it.name != null && it.typeReference != null }
                    .associate { param ->
                        val typeReference = param.typeReference!!
                        val mapped: MappedType? = KotlinToProtoMapper.resolve(typeReference)
                        val resolvedKtClass: KtClass? =
                            KtClassResolver
                                .findReferencedClass(typeReference, mapped)
                                ?.apply { stack.addLast(this) }

                        param.name!! to ClassGraph.FieldResolution(mapped, resolvedKtClass)
                    }

            classes[fqn] = ClassGraph.ResolvedClass(current, fieldResolutions)
        }

        return ClassGraph(classes)
    }
}

data class ClassGraph(
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

val KtClass.fullyQualifiedName: String
    get() = fqName?.asString() ?: name!!
