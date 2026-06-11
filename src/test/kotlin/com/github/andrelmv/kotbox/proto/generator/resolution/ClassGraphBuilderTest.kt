package com.github.andrelmv.kotbox.proto.generator.resolution

import com.github.andrelmv.kotbox.proto.generator.ProtoGeneratorTestCase
import com.github.andrelmv.kotbox.proto.generator.model.ProtoTypeMapping
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

internal class ClassGraphBuilderTest : ProtoGeneratorTestCase() {
    // -------------------------------------------------------------------------
    // Discovery
    // -------------------------------------------------------------------------

    fun `test discovers only the root for a class with scalar fields`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val name: String, val age: Int)") as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User"), graph.simpleNames())
    }

    fun `test discovers a transitively referenced data class`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val address: Address)") as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User", "Address"), graph.simpleNames())
    }

    fun `test discovers the full three-level hierarchy`() {
        myFixture.addFileToProject("Coordinates.kt", "package com.example\ndata class Coordinates(val lat: Double)")
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val coordinates: Coordinates)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val address: Address)") as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User", "Address", "Coordinates"), graph.simpleNames())
    }

    fun `test discovers a referenced enum class`() {
        myFixture.addFileToProject("Score.kt", "package com.example\nenum class Score { HIGH, LOW }")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val score: Score)") as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User", "Score"), graph.simpleNames())
    }

    fun `test discovers element class of a custom collection`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val addresses: List<Address>)") as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User", "Address"), graph.simpleNames())
    }

    fun `test discovers value class of a custom map`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val byId: Map<String, Address>)") as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User", "Address"), graph.simpleNames())
    }

    fun `test does not discover scalar-only collections or maps`() {
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "data class User(val tags: List<String>, val scores: Map<String, Int>)",
            ) as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User"), graph.simpleNames())
    }

    // -------------------------------------------------------------------------
    // De-duplication & termination
    // -------------------------------------------------------------------------

    fun `test a class referenced by two fields is discovered once`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.example\ndata class User(val home: Address, val work: Address)",
            ) as KtFile
        val graph = build(file, "User")

        assertEquals(setOf("User", "Address"), graph.simpleNames())
        assertEquals(1, graph.classes.keys.count { it.endsWith("Address") })
    }

    fun `test terminates and de-duplicates on a cyclic reference`() {
        myFixture.addFileToProject("B.kt", "package com.example\ndata class B(val a: A)")
        val file = myFixture.addFileToProject("A.kt", "package com.example\ndata class A(val b: B)") as KtFile
        val graph = build(file, "A")

        assertEquals(setOf("A", "B"), graph.simpleNames())
    }

    fun `test terminates on a self-referential class`() {
        val file = myFixture.addFileToProject("Node.kt", "package com.example\ndata class Node(val next: Node)") as KtFile
        val graph = build(file, "Node")

        assertEquals(setOf("Node"), graph.simpleNames())
    }

    // -------------------------------------------------------------------------
    // Field-resolution wiring
    // -------------------------------------------------------------------------

    fun `test field resolutions are keyed by parameter name`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val name: String, val age: Int)") as KtFile
        val resolutions = build(file, "User").rootFieldResolutions("User")

        assertEquals(setOf("name", "age"), resolutions.keys)
    }

    fun `test scalar field resolves to a ScalarType mapping with no referenced class`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val name: String)") as KtFile
        val resolution = build(file, "User").rootFieldResolutions("User").getValue("name")

        assertTrue(resolution.protoTypeMapping is ProtoTypeMapping.ScalarTypeMapping)
        assertEquals("string", (resolution.protoTypeMapping as ProtoTypeMapping.ScalarTypeMapping).type)
        assertNull(resolution.resolved)
    }

    fun `test nested data class field wires the resolved class into the resolution`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val address: Address)") as KtFile
        val resolution = build(file, "User").rootFieldResolutions("User").getValue("address")

        assertNull(resolution.protoTypeMapping) // user-defined message types map to null
        assertNotNull(resolution.resolved)
        assertEquals("Address", resolution.resolved.nameOf())
    }

    fun `test custom collection field carries CollectionType mapping and resolved element`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val addresses: List<Address>)") as KtFile
        val resolution = build(file, "User").rootFieldResolutions("User").getValue("addresses")

        assertTrue(resolution.protoTypeMapping is ProtoTypeMapping.CollectionTypeMapping)
        assertTrue((resolution.protoTypeMapping as ProtoTypeMapping.CollectionTypeMapping).customElement)
        assertEquals("Address", resolution.resolved.nameOf())
    }

    fun `test unresolvable type produces a resolution with no mapping and no class`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val mystery: GhostType)") as KtFile
        val resolution = build(file, "User").rootFieldResolutions("User").getValue("mystery")

        assertNull(resolution.protoTypeMapping)
        assertNull(resolution.resolved)
    }

    fun `test class with no fields yields an empty resolution map`() {
        val file = myFixture.addFileToProject("Empty.kt", "data class Empty()") as KtFile
        val resolutions = build(file, "Empty").rootFieldResolutions("Empty")

        assertTrue(resolutions.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun ClassGraph.simpleNames(): Set<String> = inSmartReadAction { classes.values.map { it.ktClass.name!! }.toSet() }

    private fun ClassGraph.rootFieldResolutions(simpleName: String): Map<String, ClassGraph.FieldResolution> =
        inSmartReadAction { classes.values.first { it.ktClass.name == simpleName }.fieldResolutions }

    private fun KtClass?.nameOf(): String? = inSmartReadAction { this?.name }

    private fun build(
        file: KtFile,
        rootClassName: String,
    ): ClassGraph = inSmartReadAction { ClassGraphBuilder.build(file.findClass(rootClassName)) }
}
