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

class MapResolutionRuleTest {
    @Test
    fun `produces a map field carrying the key and value types`() {
        val field = execute(MappedType.MapType(key = "string", value = "int32", customValue = false))

        assertEquals("scores", field!!.name)
        assertEquals(1, field.number)
        val map = field.fieldType as ProtoFieldType.Map
        assertEquals("string", map.keyProto)
        assertEquals("int32", map.valueProto)
    }

    @Test
    fun `passes through the nested message of a custom value`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val field = execute(MappedType.MapType(key = "string", value = "Address", customValue = true), nestedMessage = address)

        assertSame(address, field!!.nestedMessage)
    }

    @Test
    fun `ignores scalar types`() {
        assertNull(execute(MappedType.ScalarType(type = "string", isNullable = false)))
    }

    @Test
    fun `ignores collection types`() {
        assertNull(execute(MappedType.CollectionType(element = "string", customElement = false)))
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
        MapResolutionRule.tryExecute(
            name = "scores",
            typeText = "Map<String, Int>",
            number = 1,
            mappedType = mappedType,
            nestedMessage = nestedMessage,
            nestedEnum = nestedEnum,
        )
}
