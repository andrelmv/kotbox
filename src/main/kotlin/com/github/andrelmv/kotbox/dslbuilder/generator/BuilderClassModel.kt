package com.github.andrelmv.kotbox.dslbuilder.generator

data class DslBuilderClassModel(
    val dataClassName: String,
    val builderClassName: String,
    val packageName: String,
    val fields: List<DslBuilderField>,
    val dslMarkerName: String,
    val isRoot: Boolean,
)

sealed class DslBuilderField {
    abstract val name: String
    abstract val isRequired: Boolean

    data class Simple(
        override val name: String,
        val typeName: String,
        val isNullableInOriginal: Boolean,
        override val isRequired: Boolean,
    ) : DslBuilderField()

    data class NestedBuilder(
        override val name: String,
        val typeName: String,
        val builderTypeName: String,
        override val isRequired: Boolean,
    ) : DslBuilderField()

    data class SimpleList(
        override val name: String,
        val elementTypeName: String,
        override val isRequired: Boolean,
    ) : DslBuilderField()

    data class NestedBuilderList(
        override val name: String,
        val elementTypeName: String,
        val elementBuilderTypeName: String,
        override val isRequired: Boolean,
    ) : DslBuilderField()

    data class SimpleSet(
        override val name: String,
        val elementTypeName: String,
        override val isRequired: Boolean,
    ) : DslBuilderField()

    data class SimpleMap(
        override val name: String,
        val keyTypeName: String,
        val valueTypeName: String,
        override val isRequired: Boolean,
    ) : DslBuilderField()
}

data class DslBuilderHierarchy(
    /** Builders in topological order — dependencies before their dependents. Root is last. */
    val builders: List<DslBuilderClassModel>,
    val dslMarkerName: String,
    val requiredImports: Set<String>,
)
