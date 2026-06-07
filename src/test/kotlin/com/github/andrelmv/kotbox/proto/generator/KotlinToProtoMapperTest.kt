package com.github.andrelmv.kotbox.proto.generator

import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.atomic.AtomicInteger

internal class KotlinToProtoMapperTest : ProtoGeneratorTestCase() {
    // -------------------------------------------------------------------------
    // Scalars
    // -------------------------------------------------------------------------

    fun `test maps String to string`() {
        val result = resolve<MappedType.ScalarType>("String")
        assertEquals("string", result.type)
        assertFalse(result.isNullable)
    }

    fun `test maps Int to int32`() {
        assertEquals("int32", resolve<MappedType.ScalarType>("Int").type)
    }

    fun `test maps Long to int64`() {
        assertEquals("int64", resolve<MappedType.ScalarType>("Long").type)
    }

    fun `test maps Short to int32`() {
        assertEquals("int32", resolve<MappedType.ScalarType>("Short").type)
    }

    fun `test maps Byte to int32`() {
        assertEquals("int32", resolve<MappedType.ScalarType>("Byte").type)
    }

    fun `test maps Float to float`() {
        assertEquals("float", resolve<MappedType.ScalarType>("Float").type)
    }

    fun `test maps Double to double`() {
        assertEquals("double", resolve<MappedType.ScalarType>("Double").type)
    }

    fun `test maps Boolean to bool`() {
        assertEquals("bool", resolve<MappedType.ScalarType>("Boolean").type)
    }

    fun `test maps ByteArray to bytes`() {
        assertEquals("bytes", resolve<MappedType.ScalarType>("ByteArray").type)
    }

    fun `test maps Any to google protobuf Any`() {
        assertEquals("google.protobuf.Any", resolve<MappedType.ScalarType>("Any").type)
    }

    fun `test returns null for unknown scalar type`() {
        assertNull(resolveRaw("SomeCustomClass"))
    }

    // -------------------------------------------------------------------------
    // Nullable scalars
    // -------------------------------------------------------------------------

    fun `test maps nullable String to optional scalar`() {
        val result = resolve<MappedType.ScalarType>("String?")
        assertEquals("string", result.type)
        assertTrue(result.isNullable)
    }

    fun `test maps nullable Int to optional scalar`() {
        val result = resolve<MappedType.ScalarType>("Int?")
        assertEquals("int32", result.type)
        assertTrue(result.isNullable)
    }

    fun `test nullable unknown type returns null`() {
        assertNull(resolveRaw("Address?"))
    }

    fun `test nullable List of scalar is resolved as CollectionType`() {
        val result = resolve<MappedType.CollectionType>("List<String>?")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test nullable Set of scalar is resolved as CollectionType`() {
        val result = resolve<MappedType.CollectionType>("Set<Int>?")
        assertEquals("int32", result.element)
        assertFalse(result.customElement)
    }

    fun `test nullable List of custom type is resolved as CollectionType with isCustomType true`() {
        val result = resolve<MappedType.CollectionType>("List<Address>?")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test nullable Map of scalar key and scalar value is resolved as MapType`() {
        val result = resolve<MappedType.MapType>("Map<String, Int>?")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    fun `test nullable Map of scalar key and custom value is resolved as MapType`() {
        val result = resolve<MappedType.MapType>("Map<String, Address>?")
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------

    fun `test maps List of String to CollectionType with scalar element`() {
        val result = resolve<MappedType.CollectionType>("List<String>")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test maps Set of Int to CollectionType with scalar element`() {
        val result = resolve<MappedType.CollectionType>("Set<Int>")
        assertEquals("int32", result.element)
        assertFalse(result.customElement)
    }

    fun `test maps List of custom type to CollectionType with isCustomType true`() {
        val result = resolve<MappedType.CollectionType>("List<Address>")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test maps Set of custom type to CollectionType with isCustomType true`() {
        val result = resolve<MappedType.CollectionType>("Set<Address>")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test elementProto is the class name when custom type`() {
        val result = resolve<MappedType.CollectionType>("List<MyMessage>")
        assertEquals("MyMessage", result.element)
    }

    // -------------------------------------------------------------------------
    // Maps
    // -------------------------------------------------------------------------

    fun `test maps Map of String to Int`() {
        val result = resolve<MappedType.MapType>("Map<String, Int>")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    fun `test maps Map of String to custom type`() {
        val result = resolve<MappedType.MapType>("Map<String, Address>")
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    fun `test maps Map of Bool key to String value`() {
        val result = resolve<MappedType.MapType>("Map<Boolean, String>")
        assertEquals("bool", result.key)
        assertEquals("string", result.value)
    }

    fun `test returns null for Map with invalid key type`() {
        assertNull(resolveRaw("Map<Float, String>"))
    }

    fun `test returns null for Map with Double key`() {
        assertNull(resolveRaw("Map<Double, String>"))
    }

    fun `test returns null for Map with custom type key`() {
        assertNull(resolveRaw("Map<Address, String>"))
    }

    fun `test handles Map with no comma gracefully`() {
        assertNull(resolveRaw("Map<String>"))
    }

    fun `test handles nested generic in map value`() {
        val result = resolve<MappedType.MapType>("Map<String, List<Int>>")
        assertEquals("string", result.key)
        assertEquals("List<Int>", result.value)
        assertTrue(result.customValue)
    }

    fun `test findTopLevelComma correctly splits nested generic value`() {
        val result = resolve<MappedType.MapType>("Map<String, List<Int>>")
        assertEquals("string", result.key)
        assertEquals("List<Int>", result.value)
    }

    // -------------------------------------------------------------------------
    // Type aliases
    // -------------------------------------------------------------------------

    fun `test alias for scalar resolves to ScalarType`() {
        val result = aliased<MappedType.ScalarType>("typealias UserId = String", "UserId")
        assertEquals("string", result.type)
        assertFalse(result.isNullable)
    }

    fun `test alias for nullable scalar resolves to nullable ScalarType`() {
        val result = aliased<MappedType.ScalarType>("typealias Nickname = String?", "Nickname")
        assertEquals("string", result.type)
        assertTrue(result.isNullable)
    }

    fun `test alias for List of scalar resolves to CollectionType with scalar element`() {
        val result = aliased<MappedType.CollectionType>("typealias Tags = List<String>", "Tags")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test alias for List of custom type resolves to CollectionType with custom element`() {
        val result =
            aliased<MappedType.CollectionType>(
                "data class Address(val s: String)\ntypealias Addresses = List<Address>",
                "Addresses",
            )
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test alias for Map of scalars resolves to MapType`() {
        val result = aliased<MappedType.MapType>("typealias Scores = Map<String, Int>", "Scores")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    fun `test alias for Map with custom value resolves to MapType with custom value`() {
        val result =
            aliased<MappedType.MapType>(
                "data class Address(val s: String)\ntypealias ById = Map<String, Address>",
                "ById",
            )
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private inline fun <reified T : MappedType> resolve(kotlinType: String): T = resolveRaw(kotlinType).assertIs("input '$kotlinType'")

    private inline fun <reified T : MappedType> MappedType?.assertIs(context: String): T {
        assertNotNull("Expected ${T::class.simpleName} but got null for $context", this)
        assertTrue(
            "Expected ${T::class.simpleName} but got ${this!!::class.simpleName} for $context",
            this is T,
        )
        return this as T
    }

    private fun resolveRaw(kotlinType: String): MappedType? = resolveFirstParamOf("data class T(val x: $kotlinType)")

    private inline fun <reified T : MappedType> aliased(
        declarations: String,
        fieldType: String,
    ): T = resolveFirstParamOf("$declarations\ndata class T(val x: $fieldType)").assertIs("field type '$fieldType'")

    private val fileCounter = AtomicInteger()

    private fun resolveFirstParamOf(fileBody: String): MappedType? {
        val file = myFixture.addFileToProject("Test${fileCounter.incrementAndGet()}.kt", fileBody) as KtFile
        return inSmartReadAction {
            val typeRef =
                file
                    .findClass("T")
                    .primaryConstructorParameters
                    .first()
                    .typeReference!!
            KotlinToProtoMapper.resolve(typeRef)
        }
    }
}
