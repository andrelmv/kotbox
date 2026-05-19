package com.github.andrelmv.kotbox.proto.dialog

sealed interface PlacementStrategy {
    object PreviewAndCopy : PlacementStrategy

    data class NewFile(
        val fileName: String,
    ) : PlacementStrategy
}
