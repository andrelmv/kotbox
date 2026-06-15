package com.github.andrelmv.kotbox.proto.dialog

internal sealed interface ProtoPlacementStrategy {
    data object PreviewAndCopy : ProtoPlacementStrategy

    data class NewFile(
        val fileName: String,
    ) : ProtoPlacementStrategy
}
