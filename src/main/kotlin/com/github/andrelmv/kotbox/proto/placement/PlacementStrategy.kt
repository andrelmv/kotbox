package com.github.andrelmv.kotbox.proto.placement

sealed interface PlacementStrategy {
    object PreviewAndCopy : PlacementStrategy

    data class NewFile(
        val fileName: String,
    ) : PlacementStrategy
}
