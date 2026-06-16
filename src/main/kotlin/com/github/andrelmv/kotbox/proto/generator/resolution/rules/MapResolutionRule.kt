package com.github.andrelmv.kotbox.proto.generator.resolution.rules

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping

internal object MapResolutionRule : FieldResolutionRule {
    override fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        protoTypeMapping: ProtoTypeMapping?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField? {
        val map = protoTypeMapping as? ProtoTypeMapping.MapTypeMapping ?: return null
        return ProtoField(
            name = name,
            number = number,
            fieldType = ProtoFieldType.Map(map.key, map.value),
            nestedMessage = nestedMessage,
        )
    }
}
