package com.github.andrelmv.kotbox.proto.generator.rules

import com.github.andrelmv.kotbox.proto.generator.MappedType
import com.github.andrelmv.kotbox.proto.generator.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.ProtoModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScalarResolutionRuleTest {
    @Test
    fun `produces a scalar field for a non-null scalar type`() {
        val field = execute(MappedType.ScalarType(type = "string", isNullable = false))

        assertEquals("name", field!!.name)
        assertEquals(1, field.number)
        val scalar = field.fieldType as ProtoFieldType.Scalar
        assertEquals("string", scalar.protoType)
        assertEquals(ProtoModifier.NONE, scalar.modifier)
    }

    @Test
    fun `marks a nullable scalar type as optional`() {
        val field = execute(MappedType.ScalarType(type = "int32", isNullable = true))

        val scalar = field!!.fieldType as ProtoFieldType.Scalar
        assertEquals(ProtoModifier.OPTIONAL, scalar.modifier)
    }

    @Test
    fun `ignores collection types`() {
        assertNull(execute(MappedType.CollectionType(element = "string", customElement = false)))
    }

    @Test
    fun `ignores map types`() {
        assertNull(execute(MappedType.MapType(key = "string", value = "int32", customValue = false)))
    }

    @Test
    fun `ignores a null mapping`() {
        assertNull(execute(mappedType = null))
    }

    private fun execute(
        mappedType: MappedType?,
        nestedMessage: ProtoMessage? = null,
        nestedEnum: ProtoEnumModel? = null,
    ) = ScalarResolutionRule.tryExecute(
        name = "name",
        typeText = "String",
        number = 1,
        mappedType = mappedType,
        nestedMessage = nestedMessage,
        nestedEnum = nestedEnum,
    )
}
