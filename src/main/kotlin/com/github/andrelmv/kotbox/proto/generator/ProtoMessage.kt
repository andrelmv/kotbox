package com.github.andrelmv.kotbox.proto.generator

/**
 * A representation of a single proto `message` block, possibly containing nested messages.
 * Intentionally decoupled from PSI so that [CodeRenderer] has no IntelliJ dependencies.
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
) {
    override fun toString(): String = "$fieldType ${name.toSnakeCase()} = $number;"
}

sealed interface ProtoFieldType {
    data class Scalar(
        val protoType: String,
        val modifier: ProtoModifier,
    ) : ProtoFieldType {
        override fun toString(): String = "${modifier.keyword}$protoType"
    }

    data class Repeated(
        val elementProto: String,
    ) : ProtoFieldType {
        override fun toString(): String = "repeated $elementProto"
    }

    data class Map(
        val keyProto: String,
        val valueProto: String,
    ) : ProtoFieldType {
        override fun toString(): String = "map<$keyProto, $valueProto>"
    }

    data class MessageRef(
        val typeName: String,
        val modifier: ProtoModifier,
    ) : ProtoFieldType {
        override fun toString(): String = "${modifier.keyword}$typeName"
    }

    data class EnumRef(
        val typeName: String,
        val modifier: ProtoModifier,
    ) : ProtoFieldType {
        override fun toString(): String = "${modifier.keyword}$typeName"
    }
}

enum class ProtoModifier(
    val keyword: String,
) {
    NONE(""),
    OPTIONAL("optional$SPACE"),
}

data class ProtoEnumModel(
    val name: String,
    val entries: LinkedHashSet<String>,
)

private const val SPACE = " "
