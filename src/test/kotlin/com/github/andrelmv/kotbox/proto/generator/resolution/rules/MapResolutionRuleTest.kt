package com.github.andrelmv.kotbox.proto.generator.resolution.rules

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MapResolutionRuleTest {
    @Test
    fun `produces a map field carrying the key and value types`() {
        val field = execute(ProtoTypeMapping.MapTypeMapping(key = "string", value = "int32", customValue = false))

        assertEquals("scores", field!!.name)
        assertEquals(1, field.number)
        val map = field.fieldType as ProtoFieldType.Map
        assertEquals("string", map.keyProto)
        assertEquals("int32", map.valueProto)
    }

    @Test
    fun `passes through the nested message of a custom value`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val field = execute(ProtoTypeMapping.MapTypeMapping(key = "string", value = "Address", customValue = true), nestedMessage = address)

        assertSame(address, field!!.nestedMessage)
    }

    @Test
    fun `ignores scalar types`() {
        assertNull(execute(ProtoTypeMapping.ScalarTypeMapping(type = "string", isNullable = false)))
    }

    @Test
    fun `ignores collection types`() {
        assertNull(execute(ProtoTypeMapping.CollectionTypeMapping(element = "string", customElement = false)))
    }

    @Test
    fun `ignores a null mapping`() {
        assertNull(execute(protoTypeMapping = null))
    }

    private fun execute(
        protoTypeMapping: ProtoTypeMapping?,
        nestedMessage: ProtoMessage? = null,
        nestedEnum: ProtoEnumModel? = null,
    ): ProtoField? =
        MapResolutionRule.tryExecute(
            name = "scores",
            typeText = "Map<String, Int>",
            number = 1,
            protoTypeMapping = protoTypeMapping,
            nestedMessage = nestedMessage,
            nestedEnum = nestedEnum,
        )
}
