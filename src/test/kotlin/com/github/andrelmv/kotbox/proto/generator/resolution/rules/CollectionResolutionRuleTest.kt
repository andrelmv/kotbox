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

class CollectionResolutionRuleTest {
    @Test
    fun `produces a repeated field carrying the element type`() {
        val field = execute(ProtoTypeMapping.CollectionTypeMapping(element = "string", customElement = false))

        assertEquals("tags", field!!.name)
        assertEquals(1, field.number)
        assertEquals("string", (field.fieldType as ProtoFieldType.Repeated).elementProto)
    }

    @Test
    fun `passes through the nested message of a custom element`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val field = execute(ProtoTypeMapping.CollectionTypeMapping(element = "Address", customElement = true), nestedMessage = address)

        assertSame(address, field!!.nestedMessage)
    }

    @Test
    fun `leaves nested message null for a scalar element`() {
        val field = execute(ProtoTypeMapping.CollectionTypeMapping(element = "string", customElement = false))

        assertNull(field!!.nestedMessage)
    }

    @Test
    fun `ignores scalar types`() {
        assertNull(execute(ProtoTypeMapping.ScalarTypeMapping(type = "string", isNullable = false)))
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
    ): ProtoField? =
        CollectionResolutionRule.tryExecute(
            name = "tags",
            typeText = "List<String>",
            number = 1,
            protoTypeMapping = protoTypeMapping,
            nestedMessage = nestedMessage,
            nestedEnum = nestedEnum,
        )
}
