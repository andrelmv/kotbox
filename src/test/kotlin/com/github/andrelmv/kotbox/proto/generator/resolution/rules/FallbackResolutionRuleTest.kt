package com.github.andrelmv.kotbox.proto.generator.resolution.rules

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackResolutionRuleTest {
    @Test
    fun `wraps a nested message as a message ref`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val field = execute(typeText = "Address", nestedMessage = address)

        val ref = field.fieldType as ProtoFieldType.MessageRef
        assertEquals("Address", ref.typeName)
        assertEquals(ProtoModifier.NONE, ref.modifier)
        assertSame(address, field.nestedMessage)
        assertFalse(field.unresolved)
    }

    @Test
    fun `marks a nullable message ref as optional`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val field = execute(typeText = "Address?", nestedMessage = address)

        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.MessageRef).modifier)
    }

    @Test
    fun `wraps a nested enum as an enum ref`() {
        val score = ProtoEnumModel(name = "Score", entries = linkedSetOf("HIGH", "LOW"))
        val field = execute(typeText = "Score", nestedEnum = score)

        assertEquals("Score", (field.fieldType as ProtoFieldType.EnumRef).typeName)
        assertSame(score, field.nestedEnum)
    }

    @Test
    fun `prefers a nested message over a nested enum when both are present`() {
        val address = ProtoMessage(name = "Address", fields = emptyList())
        val score = ProtoEnumModel(name = "Score", entries = linkedSetOf("HIGH"))
        val field = execute(typeText = "Address", nestedMessage = address, nestedEnum = score)

        assertTrue(field.fieldType is ProtoFieldType.MessageRef)
    }

    @Test
    fun `produces an unresolved message ref when nothing is resolved`() {
        val field = execute(typeText = "SomeExternalType")

        val ref = field.fieldType as ProtoFieldType.MessageRef
        assertEquals("SomeExternalType", ref.typeName)
        assertTrue(field.unresolved)
    }

    @Test
    fun `strips the nullability marker from an unresolved type name`() {
        val field = execute(typeText = "SomeExternalType?")

        assertEquals("SomeExternalType", (field.fieldType as ProtoFieldType.MessageRef).typeName)
    }

    @Test
    fun `leaves an unresolved field non-optional even when the type is nullable`() {
        val field = execute(typeText = "SomeExternalType?")

        assertEquals(ProtoModifier.NONE, (field.fieldType as ProtoFieldType.MessageRef).modifier)
        assertTrue(field.unresolved)
    }

    @Test
    fun `handles whitespace-padded nullable types`() {
        val address = ProtoMessage(name = "Foo", fields = emptyList())
        val field = execute(typeText = "  Foo?  ", nestedMessage = address)

        val ref = field.fieldType as ProtoFieldType.MessageRef
        assertEquals("Foo", ref.typeName)
        assertEquals(ProtoModifier.OPTIONAL, ref.modifier)
    }

    private fun execute(
        typeText: String,
        nestedMessage: ProtoMessage? = null,
        nestedEnum: ProtoEnumModel? = null,
    ): ProtoField =
        FallbackResolutionRule.tryExecute(
            name = "field",
            typeText = typeText,
            number = 1,
            protoTypeMapping = null,
            nestedMessage = nestedMessage,
            nestedEnum = nestedEnum,
        )
}
