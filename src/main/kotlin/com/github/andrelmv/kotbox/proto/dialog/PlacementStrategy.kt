package com.github.andrelmv.kotbox.proto.dialog

internal sealed interface PlacementStrategy {
    object PreviewAndCopy : PlacementStrategy

    data class NewFile(
        val fileName: String,
    ) : PlacementStrategy
}
