package com.github.andrelmv.kotbox.proto.generator.rules

import com.github.andrelmv.kotbox.proto.generator.MappedType
import com.github.andrelmv.kotbox.proto.generator.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.ProtoField
import com.github.andrelmv.kotbox.proto.generator.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.ProtoMessage

internal object CollectionResolutionRule : FieldResolutionRule {
    override fun tryExecute(
        name: String,
        typeText: String,
        number: Int,
        mappedType: MappedType?,
        nestedMessage: ProtoMessage?,
        nestedEnum: ProtoEnumModel?,
    ): ProtoField? {
        val collection = mappedType as? MappedType.CollectionType ?: return null
        return ProtoField(
            name = name,
            number = number,
            fieldType = ProtoFieldType.Repeated(collection.element),
            nestedMessage = nestedMessage,
        )
    }
}
