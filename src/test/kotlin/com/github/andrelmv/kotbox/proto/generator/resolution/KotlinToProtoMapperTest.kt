package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.ProtoGeneratorTestCase
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import com.github.andrelmv.kotbox.proto.generator.resolution.KotlinToProtoMapper.resolveExpanded
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.atomic.AtomicInteger

internal class KotlinToProtoMapperTest : ProtoGeneratorTestCase() {
    // -------------------------------------------------------------------------
    // Scalars
    // -------------------------------------------------------------------------

    fun `test maps String to string`() {
        val result = resolve<ProtoTypeMapping.ScalarTypeMapping>("String")
        assertEquals("string", result.type)
        assertFalse(result.isNullable)
    }

    fun `test maps Int to int32`() {
        assertEquals("int32", resolve<ProtoTypeMapping.ScalarTypeMapping>("Int").type)
    }

    fun `test maps Long to int64`() {
        assertEquals("int64", resolve<ProtoTypeMapping.ScalarTypeMapping>("Long").type)
    }

    fun `test maps Short to int32`() {
        assertEquals("int32", resolve<ProtoTypeMapping.ScalarTypeMapping>("Short").type)
    }

    fun `test maps Byte to int32`() {
        assertEquals("int32", resolve<ProtoTypeMapping.ScalarTypeMapping>("Byte").type)
    }

    fun `test maps Float to float`() {
        assertEquals("float", resolve<ProtoTypeMapping.ScalarTypeMapping>("Float").type)
    }

    fun `test maps Double to double`() {
        assertEquals("double", resolve<ProtoTypeMapping.ScalarTypeMapping>("Double").type)
    }

    fun `test maps Boolean to bool`() {
        assertEquals("bool", resolve<ProtoTypeMapping.ScalarTypeMapping>("Boolean").type)
    }

    fun `test maps ByteArray to bytes`() {
        assertEquals(
            "bytes",
            resolve<ProtoTypeMapping.ScalarTypeMapping>("ByteArray").type,
        )
    }

    fun `test maps Any to google protobuf Any`() {
        assertEquals(
            "google.protobuf.Any",
            resolve<ProtoTypeMapping.ScalarTypeMapping>("Any").type,
        )
    }

    fun `test returns null for unknown scalar type`() {
        assertNull(resolveRaw("SomeCustomClass"))
    }

    // -------------------------------------------------------------------------
    // Nullable scalars
    // -------------------------------------------------------------------------

    fun `test maps nullable String to optional scalar`() {
        val result = resolve<ProtoTypeMapping.ScalarTypeMapping>("String?")
        assertEquals("string", result.type)
        assertTrue(result.isNullable)
    }

    fun `test maps nullable Int to optional scalar`() {
        val result = resolve<ProtoTypeMapping.ScalarTypeMapping>("Int?")
        assertEquals("int32", result.type)
        assertTrue(result.isNullable)
    }

    fun `test nullable unknown type returns null`() {
        assertNull(resolveRaw("Address?"))
    }

    fun `test nullable List of scalar is resolved as CollectionType`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("List<String>?")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test nullable Set of scalar is resolved as CollectionType`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("Set<Int>?")
        assertEquals("int32", result.element)
        assertFalse(result.customElement)
    }

    fun `test nullable List of custom type is resolved as CollectionType with isCustomType true`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("List<Address>?")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test nullable Map of scalar key and scalar value is resolved as MapType`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, Int>?")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    fun `test nullable Map of scalar key and custom value is resolved as MapType`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, Address>?")
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    fun `test List with nullable scalar element strips nullability`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("List<String?>")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test Map with nullable scalar value strips nullability`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, Int?>")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------

    fun `test maps List of String to CollectionType with scalar element`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("List<String>")
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test maps Set of Int to CollectionType with scalar element`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("Set<Int>")
        assertEquals("int32", result.element)
        assertFalse(result.customElement)
    }

    fun `test maps List of custom type to CollectionType with isCustomType true`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("List<Address>")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test maps Set of custom type to CollectionType with isCustomType true`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("Set<Address>")
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test elementProto is the class name when custom type`() {
        val result = resolve<ProtoTypeMapping.CollectionTypeMapping>("List<MyMessage>")
        assertEquals("MyMessage", result.element)
    }

    // -------------------------------------------------------------------------
    // Maps
    // -------------------------------------------------------------------------

    fun `test maps Map of String to Int`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, Int>")
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    fun `test maps Map of String to custom type`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, Address>")
        assertEquals("string", result.key)
        assertEquals("Address", result.value)
        assertTrue(result.customValue)
    }

    fun `test maps Map of Bool key to String value`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<Boolean, String>")
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
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, List<Int>>")
        assertEquals("string", result.key)
        assertEquals("List<Int>", result.value)
        assertTrue(result.customValue)
    }

    fun `test findTopLevelComma correctly splits nested generic value`() {
        val result = resolve<ProtoTypeMapping.MapTypeMapping>("Map<String, List<Int>>")
        assertEquals("string", result.key)
        assertEquals("List<Int>", result.value)
    }

    // -------------------------------------------------------------------------
    // Type aliases
    // -------------------------------------------------------------------------

    fun `test alias for scalar resolves to ScalarType`() {
        val result =
            aliased<ProtoTypeMapping.ScalarTypeMapping>(
                "typealias UserId = String",
                "UserId",
            )
        assertEquals("string", result.type)
        assertFalse(result.isNullable)
    }

    fun `test alias for nullable scalar resolves to nullable ScalarType`() {
        val result =
            aliased<ProtoTypeMapping.ScalarTypeMapping>(
                "typealias Nickname = String?",
                "Nickname",
            )
        assertEquals("string", result.type)
        assertTrue(result.isNullable)
    }

    fun `test alias for List of scalar resolves to CollectionType with scalar element`() {
        val result =
            aliased<ProtoTypeMapping.CollectionTypeMapping>(
                "typealias Tags = List<String>",
                "Tags",
            )
        assertEquals("string", result.element)
        assertFalse(result.customElement)
    }

    fun `test alias for List of custom type resolves to CollectionType with custom element`() {
        val result =
            aliased<ProtoTypeMapping.CollectionTypeMapping>(
                "data class Address(val s: String)\ntypealias Addresses = List<Address>",
                "Addresses",
            )
        assertEquals("Address", result.element)
        assertTrue(result.customElement)
    }

    fun `test alias for Map of scalars resolves to MapType`() {
        val result =
            aliased<ProtoTypeMapping.MapTypeMapping>(
                "typealias Scores = Map<String, Int>",
                "Scores",
            )
        assertEquals("string", result.key)
        assertEquals("int32", result.value)
        assertFalse(result.customValue)
    }

    fun `test alias for Map with custom value resolves to MapType with custom value`() {
        val result =
            aliased<ProtoTypeMapping.MapTypeMapping>(
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

    private inline fun <reified T : ProtoTypeMapping> resolve(kotlinType: String): T =
        resolveRaw(kotlinType).assertIs("input '$kotlinType'")

    private inline fun <reified T : ProtoTypeMapping> ProtoTypeMapping?.assertIs(context: String): T {
        assertNotNull("Expected ${T::class.simpleName} but got null for $context", this)
        assertTrue(
            "Expected ${T::class.simpleName} but got ${this!!::class.simpleName} for $context",
            this is T,
        )
        return this as T
    }

    private fun resolveRaw(kotlinType: String): ProtoTypeMapping? = resolveFirstParamOf("data class T(val x: $kotlinType)")

    private inline fun <reified T : ProtoTypeMapping> aliased(
        declarations: String,
        fieldType: String,
    ): T = resolveFirstParamOf("$declarations\ndata class T(val x: $fieldType)").assertIs("field type '$fieldType'")

    private val fileCounter = AtomicInteger()

    @OptIn(KaExperimentalApi::class)
    private fun resolveFirstParamOf(fileBody: String): ProtoTypeMapping? {
        val file = myFixture.addFileToProject("Test${fileCounter.incrementAndGet()}.kt", fileBody) as KtFile
        return inSmartReadAction {
            val typeRef =
                file
                    .findClass("T")
                    .primaryConstructorParameters
                    .first()
                    .typeReference!!
            KotlinToProtoMapper
                .resolveFromText(typeRef.text)
                ?: analyze(typeRef) { resolveExpanded(typeRef) }
        }
    }
}
