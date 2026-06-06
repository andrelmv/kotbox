package com.github.andrelmv.kotbox.proto.generator.rules

import com.github.andrelmv.kotbox.proto.generator.MappedType
import com.github.andrelmv.kotbox.proto.generator.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.ProtoField
import com.github.andrelmv.kotbox.proto.generator.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.ProtoModifier

class FallbackResolutionRule : FieldResolutionRule {
    override fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        mappedType: MappedType?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField {
        val isNullable = typeText.endsWith('?')
        val baseType = typeText.trimEnd('?').trim()
        val modifier = if (isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE

        nestedMessage?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.MessageRef(it.name, modifier),
                nestedMessage = it,
            )
        }

        nestedEnum?.let {
            return ProtoField(
                name = name,
                number = number,
                fieldType = ProtoFieldType.EnumRef(it.name, modifier),
                nestedEnum = it,
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
