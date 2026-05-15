package com.github.andrelmv.kotbox.proto.generator

/**
 * An in-memory representation of a single proto `message` block, possibly
 * containing nested messages. Intentionally decoupled from PSI so that
 * [ProtoRenderer] has no IntelliJ dependencies.
 */
data class ProtoMessageModel(
    val name: String,
    val fields: List<ProtoField>,
)

data class ProtoField(
    val fieldType: ProtoFieldType,
    val name: String,
    val number: Int,
    val nestedMessage: ProtoMessageModel?,
    val nestedEnum: ProtoEnumModel?,
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
}

enum class ProtoModifier(
    val keyword: String,
) {
    NONE(""),
    OPTIONAL("optional"),
}

@DslMarker
annotation class FieldDsl

@FieldDsl
class FieldBuilder {
    var name: String? = null
    var number: Int? = null
    var fieldType: ProtoFieldType? = null
    var nestedMessage: ProtoMessageModel? = null
    var nestedEnum: ProtoEnumModel? = null
    var unresolved: Boolean = false

    fun build(): ProtoField =
        ProtoField(
            fieldType = fieldType ?: error("FieldBuilder: 'fieldType' is required"),
            name = name ?: error("FieldBuilder: 'name' is required"),
            number = number ?: error("FieldBuilder: 'number' is required"),
            nestedMessage = nestedMessage,
            nestedEnum = nestedEnum,
            unresolved = unresolved,
        )
}

fun field(block: FieldBuilder.() -> Unit) = FieldBuilder().apply(block).build()

data class ProtoEnumModel(
    val name: String,
    val entries: Set<String>,
)
