package com.github.andrelmv.kotbox.proto.generator

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

internal class ProtoTypeMapperTest {
    // -------------------------------------------------------------------------
    // Scalars
    // -------------------------------------------------------------------------

    @Test
    fun `test maps String to string`() {
        val result = resolve<MappedProtoType.Scalar>("String")
        assertEquals("string", result.protoType)
        assertFalse(result.isNullable)
    }

    @Test
    fun `test maps Int to int32`() {
        assertEquals("int32", resolve<MappedProtoType.Scalar>("Int").protoType)
    }

    @Test
    fun `test maps Long to int64`() {
        assertEquals("int64", resolve<MappedProtoType.Scalar>("Long").protoType)
    }

    @Test
    fun `test maps Short to int32`() {
        assertEquals("int32", resolve<MappedProtoType.Scalar>("Short").protoType)
    }

    @Test
    fun `test maps Byte to int32`() {
        assertEquals("int32", resolve<MappedProtoType.Scalar>("Byte").protoType)
    }

    @Test
    fun `test maps Float to float`() {
        assertEquals("float", resolve<MappedProtoType.Scalar>("Float").protoType)
    }

    @Test
    fun `test maps Double to double`() {
        assertEquals("double", resolve<MappedProtoType.Scalar>("Double").protoType)
    }

    @Test
    fun `test maps Boolean to bool`() {
        assertEquals("bool", resolve<MappedProtoType.Scalar>("Boolean").protoType)
    }

    @Test
    fun `test maps ByteArray to bytes`() {
        assertEquals("bytes", resolve<MappedProtoType.Scalar>("ByteArray").protoType)
    }

    @Test
    fun `test maps Any to google protobuf Any`() {
        assertEquals("google.protobuf.Any", resolve<MappedProtoType.Scalar>("Any").protoType)
    }

    @Test
    fun `test returns null for unknown scalar type`() {
        assertNull(ProtoTypeMapper.resolve("SomeCustomClass"))
    }

    @Test
    fun `test trims whitespace before resolving`() {
        assertEquals("string", resolve<MappedProtoType.Scalar>("  String  ").protoType)
    }

    // -------------------------------------------------------------------------
    // Nullable scalars
    // -------------------------------------------------------------------------

    @Test
    fun `test maps nullable String to optional scalar`() {
        val result = resolve<MappedProtoType.Scalar>("String?")
        assertEquals("string", result.protoType)
        assertTrue(result.isNullable)
    }

    @Test
    fun `test maps nullable Int to optional scalar`() {
        val result = resolve<MappedProtoType.Scalar>("Int?")
        assertEquals("int32", result.protoType)
        assertTrue(result.isNullable)
    }

    @Test
    fun `test nullable unknown type returns null`() {
        assertNull(ProtoTypeMapper.resolve("Address?"))
    }

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------

    @Test
    fun `test maps List of String to CollectionType with scalar element`() {
        val result = resolve<MappedProtoType.CollectionType>("List<String>")
        assertEquals("string", result.elementProto)
        assertFalse(result.isCustomType)
    }

    @Test
    fun `test maps Set of Int to CollectionType with scalar element`() {
        val result = resolve<MappedProtoType.CollectionType>("Set<Int>")
        assertEquals("int32", result.elementProto)
        assertFalse(result.isCustomType)
    }

    @Test
    fun `test maps List of custom type to CollectionType with isCustomType true`() {
        val result = resolve<MappedProtoType.CollectionType>("List<Address>")
        assertEquals("Address", result.elementProto)
        assertTrue(result.isCustomType)
    }

    @Test
    fun `test maps Set of custom type to CollectionType with isCustomType true`() {
        val result = resolve<MappedProtoType.CollectionType>("Set<Address>")
        assertEquals("Address", result.elementProto)
        assertTrue(result.isCustomType)
    }

    @Test
    fun `test elementProto is the class name when custom type`() {
        val result = resolve<MappedProtoType.CollectionType>("List<MyMessage>")
        assertEquals("MyMessage", result.elementProto)
    }

    // -------------------------------------------------------------------------
    // Maps
    // -------------------------------------------------------------------------

    @Test
    fun `test maps Map of String to Int`() {
        val result = resolve<MappedProtoType.MapType>("Map<String, Int>")
        assertEquals("string", result.keyProto)
        assertEquals("int32", result.valueProto)
        assertFalse(result.isCustomValue)
    }

    @Test
    fun `test maps Map of String to custom type`() {
        val result = resolve<MappedProtoType.MapType>("Map<String, Address>")
        assertEquals("string", result.keyProto)
        assertEquals("Address", result.valueProto)
        assertTrue(result.isCustomValue)
    }

    @Test
    fun `test maps Map of Bool key to String value`() {
        val result = resolve<MappedProtoType.MapType>("Map<Boolean, String>")
        assertEquals("bool", result.keyProto)
        assertEquals("string", result.valueProto)
    }

    @Test
    fun `test returns null for Map with invalid key type`() {
        // Float is not a valid proto map key
        assertNull(ProtoTypeMapper.resolve("Map<Float, String>"))
    }

    @Test
    fun `test returns null for Map with Double key`() {
        assertNull(ProtoTypeMapper.resolve("Map<Double, String>"))
    }

    @Test
    fun `test returns null for Map with custom type key`() {
        // Message types are not valid proto map keys
        assertNull(ProtoTypeMapper.resolve("Map<Address, String>"))
    }

    @Test
    fun `test handles Map with no comma gracefully`() {
        assertNull(ProtoTypeMapper.resolve("Map<String>"))
    }

    @Test
    fun `test handles nested generic in map value`() {
        // Map<String, List<Int>> — valueKotlin is "List<Int>", not a scalar
        val result = resolve<MappedProtoType.MapType>("Map<String, List<Int>>")
        assertEquals("string", result.keyProto)
        // List<Int> is not in scalarMap so valueProto falls back to the kotlin type name
        assertEquals("List<Int>", result.valueProto)
        assertTrue(result.isCustomValue)
    }

    @Test
    fun `test findTopLevelComma correctly splits nested generic value`() {
        // The comma inside List<Int> must not be treated as the map separator
        val result = resolve<MappedProtoType.MapType>("Map<String, List<Int>>")
        assertEquals("string", result.keyProto)
        assertEquals("List<Int>", result.valueProto)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `test empty string returns null`() {
        assertNull(ProtoTypeMapper.resolve(""))
    }

    @Test
    fun `test whitespace only returns null`() {
        assertNull(ProtoTypeMapper.resolve("   "))
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private inline fun <reified T : MappedProtoType> resolve(kotlinType: String): T {
        val result = ProtoTypeMapper.resolve(kotlinType)
        assertNotNull("Expected ${T::class.simpleName} but got null for input '$kotlinType'", result)
        assertTrue(
            "Expected ${T::class.simpleName} but got ${result!!::class.simpleName}",
            result is T,
        )
        return result as T
    }
}
