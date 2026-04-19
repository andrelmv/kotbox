package com.github.andrelmv.kotbox.dslbuilder.generator

data class BuilderClassModel(
    val dataClassName: String,
    val builderClassName: String,
    val packageName: String,
    val fields: List<BuilderField>,
    val dslMarkerName: String,
    val isRoot: Boolean,
)

sealed class BuilderField {
    abstract val name: String
    abstract val isRequired: Boolean

    data class Simple(
        override val name: String,
        val typeName: String,
        val isNullableInOriginal: Boolean,
        override val isRequired: Boolean,
    ) : BuilderField()

    data class NestedBuilder(
        override val name: String,
        val typeName: String,
        val builderTypeName: String,
        override val isRequired: Boolean,
    ) : BuilderField()

    data class SimpleList(
        override val name: String,
        val elementTypeName: String,
        override val isRequired: Boolean,
    ) : BuilderField()

    data class NestedBuilderList(
        override val name: String,
        val elementTypeName: String,
        val elementBuilderTypeName: String,
        override val isRequired: Boolean,
    ) : BuilderField()

    data class SimpleSet(
        override val name: String,
        val elementTypeName: String,
        override val isRequired: Boolean,
    ) : BuilderField()

    data class SimpleMap(
        override val name: String,
        val keyTypeName: String,
        val valueTypeName: String,
        override val isRequired: Boolean,
    ) : BuilderField()
}

data class BuilderHierarchy(
    /** Builders in topological order — dependencies before their dependents. Root is last. */
    val builders: List<BuilderClassModel>,
    val dslMarkerName: String,
    val requiredImports: Set<String>,
)
