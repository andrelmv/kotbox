package com.github.andrelmv.kotbox.dslbuilder.placement

sealed class DslBuilderPlacementStrategy {
    data object SameFile : DslBuilderPlacementStrategy()

    data class NewFile(
        val fileName: String,
    ) : DslBuilderPlacementStrategy()
}
