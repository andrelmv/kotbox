package com.github.andrelmv.kotbox.dslbuilder.placement

sealed class PlacementStrategy {
    data object SameFile : PlacementStrategy()

    data class NewFile(
        val fileName: String,
    ) : PlacementStrategy()
}
