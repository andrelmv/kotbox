package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.ProtoGeneratorTestCase
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.atomic.AtomicInteger

internal class FieldResolutionFacadeTest : ProtoGeneratorTestCase() {
    fun `test scalar maps and resolves no class`() {
        val probe = probe("data class T(val x: String)")

        assertEquals(ProtoTypeMapping.ScalarTypeMapping(type = "string", isNullable = false), probe.typeMapping)
        assertNull(probe.resolvedName)
    }

    fun `test scalar collection maps and resolves no class`() {
        val probe = probe("data class T(val x: List<Int>)")

        assertEquals(ProtoTypeMapping.CollectionTypeMapping(element = "int32", customElement = false), probe.typeMapping)
        assertNull(probe.resolvedName)
    }

    fun `test scalar-valued map maps and resolves no class`() {
        val probe = probe("data class T(val x: Map<String, Int>)")

        assertEquals(ProtoTypeMapping.MapTypeMapping(key = "string", value = "int32", customValue = false), probe.typeMapping)
        assertNull(probe.resolvedName)
    }

    fun `test custom-element collection maps and resolves element class`() {
        val probe = probe("data class Address(val street: String)\ndata class T(val x: List<Address>)")

        assertEquals(ProtoTypeMapping.CollectionTypeMapping(element = "Address", customElement = true), probe.typeMapping)
        assertEquals("Address", probe.resolvedName)
    }

    fun `test custom-value map maps and resolves value class`() {
        val probe = probe("data class Address(val street: String)\ndata class T(val x: Map<String, Address>)")

        assertEquals(ProtoTypeMapping.MapTypeMapping(key = "string", value = "Address", customValue = true), probe.typeMapping)
        assertEquals("Address", probe.resolvedName)
    }

    fun `test data class reference has no mapping and resolves the class`() {
        val probe = probe("data class Address(val street: String)\ndata class T(val x: Address)")

        assertNull(probe.typeMapping)
        assertEquals("Address", probe.resolvedName)
        assertTrue(probe.resolvedIsData)
    }

    fun `test enum reference has no mapping and resolves the enum`() {
        val probe = probe("enum class Color { RED, GREEN }\ndata class T(val x: Color)")

        assertNull(probe.typeMapping)
        assertEquals("Color", probe.resolvedName)
        assertTrue(probe.resolvedIsEnum)
    }

    fun `test unknown non-source type has no mapping and resolves no class`() {
        val probe = probe("interface Marker\ndata class T(val x: Marker)")

        assertNull(probe.typeMapping)
        assertNull(probe.resolvedName)
    }

    // -------------------------------------------------------------------------
    // Type aliases (K2 expansion fallback)
    // -------------------------------------------------------------------------

    fun `test scalar type alias maps and resolves no class`() {
        val probe = probe("typealias UserId = String\ndata class T(val x: UserId)")

        assertEquals(ProtoTypeMapping.ScalarTypeMapping(type = "string", isNullable = false), probe.typeMapping)
        assertNull(probe.resolvedName)
    }

    fun `test collection type alias maps and resolves element class via expansion`() {
        val probe =
            probe(
                "data class Address(val street: String)\ntypealias AddressList = List<Address>\ndata class T(val x: AddressList)",
            )

        assertEquals(ProtoTypeMapping.CollectionTypeMapping(element = "Address", customElement = true), probe.typeMapping)
        assertEquals("Address", probe.resolvedName)
    }

    // PSI-derived facts captured inside the read action, safe for assertion
    private data class Probe(
        val typeMapping: ProtoTypeMapping?,
        val resolvedName: String?,
        val resolvedIsData: Boolean,
        val resolvedIsEnum: Boolean,
    )

    private val fileCounter = AtomicInteger()

    private fun probe(fileBody: String): Probe {
        val file = myFixture.addFileToProject("Test${fileCounter.incrementAndGet()}.kt", fileBody) as KtFile
        return inSmartReadAction {
            val typeRef =
                file
                    .findClass("T")
                    .primaryConstructorParameters
                    .first()
                    .typeReference!!
            val resolution = FieldResolutionFacade.resolve(typeRef)
            Probe(
                typeMapping = resolution.protoTypeMapping,
                resolvedName = resolution.resolved?.name,
                resolvedIsData = resolution.resolved?.isData() == true,
                resolvedIsEnum = resolution.resolved?.isEnum() == true,
            )
        }
    }
}
