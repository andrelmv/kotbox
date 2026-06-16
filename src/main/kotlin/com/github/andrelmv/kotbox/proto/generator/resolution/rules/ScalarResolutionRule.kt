package com.github.andrelmv.kotbox.proto.generator.resolution.rules

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping

internal object ScalarResolutionRule : FieldResolutionRule {
    override fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        protoTypeMapping: ProtoTypeMapping?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField? {
        val scalar = protoTypeMapping as? ProtoTypeMapping.ScalarTypeMapping ?: return null
        return ProtoField(
            name = name,
            number = number,
            fieldType =
                ProtoFieldType.Scalar(
                    protoType = scalar.type,
                    modifier = if (scalar.isNullable) ProtoModifier.OPTIONAL else ProtoModifier.NONE,
                ),
        )
    }
}
