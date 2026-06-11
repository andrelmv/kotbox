package com.github.andrelmv.kotbox.proto.generator.model

/**
 * Data model for a proto `message` block — no IntelliJ dependencies
 */
data class ProtoMessage(
    val name: String,
    val fields: List<ProtoField>,
)

data class ProtoField(
    val fieldType: ProtoFieldType,
    val name: String,
    val number: Int,
    val nestedMessage: ProtoMessage? = null,
    val nestedEnum: ProtoEnumModel? = null,
    val unresolved: Boolean = false,
)

sealed interface ProtoFieldType {
    data class Scalar(
        val protoType: String,
        val modifier: ProtoModifier,
    ) : ProtoFieldType

    data class Repeated(
        val elementProto: String,
    ) : ProtoFieldType

    data class Map(
        val keyProto: String,
        val valueProto: String,
    ) : ProtoFieldType

    data class MessageRef(
        val typeName: String,
        val modifier: ProtoModifier,
    ) : ProtoFieldType

    data class EnumRef(
        val typeName: String,
        val modifier: ProtoModifier,
    ) : ProtoFieldType
}

enum class ProtoModifier {
    NONE,
    OPTIONAL,
}

data class ProtoEnumModel(
    val name: String,
    val entries: LinkedHashSet<String>,
)
