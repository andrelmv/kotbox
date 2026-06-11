package com.github.andrelmv.kotbox.proto.generator.resolution.rules

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping

internal object FallbackResolutionRule : FieldResolutionRule {
    override fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        protoTypeMapping: ProtoTypeMapping?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField {
        val trimmed = typeText.trim()
        val isNullable = trimmed.endsWith('?')
        val baseType = trimmed.removeSuffix("?").trim()
        val modifier = if (isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE

        if (nestedMessage != null) {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.MessageRef(nestedMessage.name, modifier),
                nestedMessage = nestedMessage,
            )
        }

        if (nestedEnum != null) {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.EnumRef(nestedEnum.name, modifier),
                nestedEnum = nestedEnum,
            )
        }

        return ProtoField(
            name = name,
            number = number,
            fieldType = ProtoFieldType.MessageRef(baseType, ProtoModifier.NONE),
            unresolved = true,
        )
    }
}
