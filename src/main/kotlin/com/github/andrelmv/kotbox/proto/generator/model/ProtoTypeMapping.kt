package com.github.andrelmv.kotbox.proto.generator.model

internal sealed interface ProtoTypeMapping {
    data class ScalarTypeMapping(
        val type: String,
        val isNullable: Boolean,
    ) : ProtoTypeMapping

    data class MapTypeMapping(
        val key: String,
        val value: String,
        val customValue: Boolean,
    ) : ProtoTypeMapping

    data class CollectionTypeMapping(
        val element: String,
        val customElement: Boolean,
    ) : ProtoTypeMapping
}
