package com.github.andrelmv.kotbox.proto.generator.rules

import com.github.andrelmv.kotbox.proto.generator.MappedType
import com.github.andrelmv.kotbox.proto.generator.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.ProtoField
import com.github.andrelmv.kotbox.proto.generator.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.ProtoMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CollectionResolutionRuleTest {
    @Test
    fun `produces a repeated field carrying the element type`() {
        val field = execute(MappedType.CollectionType(element = "string", customElement = false))

        assertEquals("tags", field!!.name)
        assertEquals(1, field.number)
        assertEquals("string", (field.fieldType as ProtoFieldType.Repeated).elementProto)
    }

    @Test
    fun `passes through the nested message of a custom element`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val field = execute(MappedType.CollectionType(element = "Address", customElement = true), nestedMessage = address)

        assertSame(address, field!!.nestedMessage)
    }

    @Test
    fun `leaves nested message null for a scalar element`() {
        val field = execute(MappedType.CollectionType(element = "string", customElement = false))

        assertNull(field!!.nestedMessage)
    }

    @Test
    fun `ignores scalar types`() {
        assertNull(execute(MappedType.ScalarType(type = "string", isNullable = false)))
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
    ): ProtoField? =
        CollectionResolutionRule.tryExecute(
            name = "tags",
            typeText = "List<String>",
            number = 1,
            mappedType = mappedType,
            nestedMessage = nestedMessage,
            nestedEnum = nestedEnum,
        )
}
