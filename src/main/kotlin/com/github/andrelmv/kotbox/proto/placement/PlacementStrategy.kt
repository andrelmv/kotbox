package com.github.andrelmv.kotbox.proto.placement

sealed interface PlacementStrategy {
    object SameFile : PlacementStrategy

    data class NewFile(
        val fileName: String,
    ) : PlacementStrategy
}
