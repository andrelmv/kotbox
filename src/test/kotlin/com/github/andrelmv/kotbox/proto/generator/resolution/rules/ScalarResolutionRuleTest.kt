package com.github.andrelmv.kotbox.proto.generator.resolution.rules

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScalarResolutionRuleTest {
    @Test
    fun `produces a scalar field for a non-null scalar type`() {
        val field = execute(ProtoTypeMapping.ScalarTypeMapping(type = "string", isNullable = false))

        assertEquals("name", field!!.name)
        assertEquals(1, field.number)
        val scalar = field.fieldType as ProtoFieldType.Scalar
        assertEquals("string", scalar.protoType)
        assertEquals(ProtoModifier.NONE, scalar.modifier)
    }

    @Test
    fun `marks a nullable scalar type as optional`() {
        val field = execute(ProtoTypeMapping.ScalarTypeMapping(type = "int32", isNullable = true))

        val scalar = field!!.fieldType as ProtoFieldType.Scalar
        assertEquals(ProtoModifier.OPTIONAL, scalar.modifier)
    }

    @Test
    fun `ignores collection types`() {
        assertNull(execute(ProtoTypeMapping.CollectionTypeMapping(element = "string", customElement = false)))
    }

    @Test
    fun `ignores map types`() {
        assertNull(execute(ProtoTypeMapping.MapTypeMapping(key = "string", value = "int32", customValue = false)))
    }

    @Test
    fun `ignores a null mapping`() {
        assertNull(execute(protoTypeMapping = null))
    }

    private fun execute(
        protoTypeMapping: ProtoTypeMapping?,
        nestedMessage: ProtoMessage? = null,
        nestedEnum: ProtoEnumModel? = null,
    ) = ScalarResolutionRule.tryExecute(
        name = "name",
        typeText = "String",
        number = 1,
        protoTypeMapping = protoTypeMapping,
        nestedMessage = nestedMessage,
        nestedEnum = nestedEnum,
    )
}
