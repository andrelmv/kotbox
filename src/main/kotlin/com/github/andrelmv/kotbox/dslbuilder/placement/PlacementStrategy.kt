package com.github.andrelmv.kotbox.dslbuilder.placement

sealed class PlacementStrategy {
    object SameFile : PlacementStrategy()

    data class NewFile(
        val fileName: String,
    ) : PlacementStrategy()
}
