package com.github.andrelmv.kotbox.proto.generator.rules

import com.github.andrelmv.kotbox.proto.generator.MappedType
import com.github.andrelmv.kotbox.proto.generator.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.ProtoField
import com.github.andrelmv.kotbox.proto.generator.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.ProtoModifier

internal object ScalarResolutionRule : FieldResolutionRule {
    override fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        mappedType: MappedType?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField? {
        val scalar = mappedType as? MappedType.ScalarType ?: return null
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
