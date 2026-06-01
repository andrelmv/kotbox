package com.github.andrelmv.kotbox.proto.generator

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

internal class KotlinToProtoMapperTest {
    // -------------------------------------------------------------------------
    // Scalars
    // -------------------------------------------------------------------------

    @Test
    fun `test maps String to string`() {
        val result = resolve<MappedType.ScalarType>("String")
        assertEquals("string", result.type)
        assertFalse(result.isNullable)
    }

    @Test
    fun `test maps Int to int32`() {
        assertEquals("int32", resolve<MappedType.ScalarType>("Int").type)
    }

    @Test
    fun `test maps Long to int64`() {
        assertEquals("int64", resolve<MappedType.ScalarType>("Long").type)
    }

    @Test
    fun `test maps Short to int32`() {
        assertEquals("int32", resolve<MappedType.ScalarType>("Short").type)
    }

    @Test
    fun `test maps Byte to int32`() {
        assertEquals("int32", resolve<MappedType.ScalarType>("Byte").type)
    }

    @Test
    fun `test maps Float to float`() {
        assertEquals("float", resolve<MappedType.ScalarType>("Float").type)
    }

    @Test
    fun `test maps Double to double`() {
        assertEquals("double", resolve<MappedType.ScalarType>("Double").type)
    }

    @Test
    fun `test maps Boolean to bool`() {
        assertEquals("bool", resolve<MappedType.ScalarType>("Boolean").type)
    }

    @Test
    fun `test maps ByteArray to bytes`() {
        assertEquals("bytes", resolve<MappedType.ScalarType>("ByteArray").type)
    }

    @Test
    fun `test maps Any to google protobuf Any`() {
        assertEquals("google.protobuf.Any", resolve<MappedType.ScalarType>("Any").type)
    }

    @Test
    fun `test returns null for unknown scalar type`() {
        assertNull(KotlinToProtoMapper.resolve("SomeCustomClass"))
    }

    @Test
    fun `test trims whitespace before resolving`() {
        assertEquals("string", resolve<MappedType.ScalarType>("  String  ").type)
    }

    // -------------------------------------------------------------------------
    // Nullable scalars
    // -------------------------------------------------------------------------

    @Test
    fun `test maps nullable String to optional scalar`() {
        val result = resolve<MappedType.ScalarType>("String?")
        assertEquals("string", result.type)
        assertTrue(result.isNullable)
    }

    @Test
    fun `test maps nullable Int to optional scalar`() {
        val result = resolve<MappedType.ScalarType>("Int?")
        assertEquals("int32", result.type)
        assertTrue(result.isNullable)
    }

    @Test
    fun `test nullable unknown type returns null`() {
        assertNull(KotlinToProtoMapper.resolve("Address?"))
    }

    @Test
    fun `test nullable List of scalar is resolved as CollectionType`() {
        val result = resolve<MappedType.CollectionType>("List<String>?")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    @Test
    fun `test nullable Set of scalar is resolved as CollectionType`() {
        val result = resolve<MappedType.CollectionType>("Set<Int>?")
        assertEquals("int32", result.element)
        assertFalse(result.customElement)
    }

    @Test
    fun `test nullable List of custom type is resolved as CollectionType with isCustomType true`() {
        val result = resolve<MappedType.CollectionType>("List<Address>?")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    @Test
    fun `test nullable Map of scalar key and scalar value is resolved as MapType`() {
        val result = resolve<MappedType.MapType>("Map<String, Int>?")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    @Test
    fun `test nullable Map of scalar key and custom value is resolved as MapType`() {
        val result = resolve<MappedType.MapType>("Map<String, Address>?")
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------

    @Test
    fun `test maps List of String to CollectionType with scalar element`() {
        val result = resolve<MappedType.CollectionType>("List<String>")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    @Test
    fun `test maps Set of Int to CollectionType with scalar element`() {
        val result = resolve<MappedType.CollectionType>("Set<Int>")
        assertEquals("int32", result.element)
        assertFalse(result.customElement)
    }

    @Test
    fun `test maps List of custom type to CollectionType with isCustomType true`() {
        val result = resolve<MappedType.CollectionType>("List<Address>")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    @Test
    fun `test maps Set of custom type to CollectionType with isCustomType true`() {
        val result = resolve<MappedType.CollectionType>("Set<Address>")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    @Test
    fun `test elementProto is the class name when custom type`() {
        val result = resolve<MappedType.CollectionType>("List<MyMessage>")
        assertEquals("MyMessage", result.element)
    }

    // -------------------------------------------------------------------------
    // Maps
    // -------------------------------------------------------------------------

    @Test
    fun `test maps Map of String to Int`() {
        val result = resolve<MappedType.MapType>("Map<String, Int>")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    @Test
    fun `test maps Map of String to custom type`() {
        val result = resolve<MappedType.MapType>("Map<String, Address>")
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    @Test
    fun `test maps Map of Bool key to String value`() {
        val result = resolve<MappedType.MapType>("Map<Boolean, String>")
        assertEquals("bool", result.key)
        assertEquals("string", result.value)
    }

    @Test
    fun `test returns null for Map with invalid key type`() {
        // Float is not a valid proto map key
        assertNull(KotlinToProtoMapper.resolve("Map<Float, String>"))
    }

    @Test
    fun `test returns null for Map with Double key`() {
        assertNull(KotlinToProtoMapper.resolve("Map<Double, String>"))
    }

    @Test
    fun `test returns null for Map with custom type key`() {
        // Message types are not valid proto map keys
        assertNull(KotlinToProtoMapper.resolve("Map<Address, String>"))
    }

    @Test
    fun `test handles Map with no comma gracefully`() {
        assertNull(KotlinToProtoMapper.resolve("Map<String>"))
    }

    @Test
    fun `test handles nested generic in map value`() {
        // Map<String, List<Int>> — valueKotlin is "List<Int>", not a scalar
        val result = resolve<MappedType.MapType>("Map<String, List<Int>>")
        assertEquals("string", result.key)
        // List<Int> is not in scalarMap so value falls back to the kotlin type name
        assertEquals("List<Int>", result.value)
        assertTrue(result.customValue)
    }

    @Test
    fun `test findTopLevelComma correctly splits nested generic value`() {
        // The comma inside List<Int> must not be treated as the map separator
        val result = resolve<MappedType.MapType>("Map<String, List<Int>>")
        assertEquals("string", result.key)
        assertEquals("List<Int>", result.value)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `test empty string returns null`() {
        assertNull(KotlinToProtoMapper.resolve(""))
    }

    @Test
    fun `test whitespace only returns null`() {
        assertNull(KotlinToProtoMapper.resolve("   "))
    }

    private inline fun <reified T : MappedType> resolve(kotlinType: String): T {
        val result = KotlinToProtoMapper.resolve(kotlinType)
        assertNotNull("Expected ${T::class.simpleName} but got null for input '$kotlinType'", result)
        assertTrue(
            "Expected ${T::class.simpleName} but got ${result!!::class.simpleName}",
            result is T,
        )
        return result as T
    }
}
