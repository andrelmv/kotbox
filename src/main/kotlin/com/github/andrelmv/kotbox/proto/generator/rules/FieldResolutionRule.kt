package com.github.andrelmv.kotbox.proto.generator.rules

import com.github.andrelmv.kotbox.proto.generator.MappedType
import com.github.andrelmv.kotbox.proto.generator.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.ProtoField
import com.github.andrelmv.kotbox.proto.generator.ProtoMessage

internal interface FieldResolutionRule {
    fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        mappedType: MappedType?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField?
}
