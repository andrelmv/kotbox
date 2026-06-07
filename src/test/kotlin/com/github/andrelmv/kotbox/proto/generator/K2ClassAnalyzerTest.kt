package com.github.andrelmv.kotbox.proto.generator

import org.jetbrains.kotlin.psi.KtFile
import kotlin.test.assertFailsWith

internal class K2ClassAnalyzerTest : ProtoGeneratorTestCase() {
    fun `test rejects a non-data class`() {
        val file = myFixture.addFileToProject("Plain.kt", "class Plain(val name: String)") as KtFile
        val error =
            inSmartReadAction {
                assertFailsWith<IllegalArgumentException> {
                    K2ClassAnalyzer().analyze(file.findClass("Plain"))
                }
            }

        assertTrue(error.message!!.contains("not a data class"))
    }

    // -------------------------------------------------------------------------
    // Field building
    // -------------------------------------------------------------------------

    fun `test builds a message with sequentially numbered fields`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val name: String, val age: Int)") as KtFile
        val model = analyze(file, "User")

        assertEquals("User", model.name)
        assertEquals(listOf("name", "age"), model.fields.map { it.name })
        assertEquals(listOf(1, 2), model.fields.map { it.number })
        assertTrue(model.fields[0].fieldType is ProtoFieldType.Scalar)
    }

    fun `test produces an empty field list for a class with no fields`() {
        val file = myFixture.addFileToProject("Empty.kt", "data class Empty()") as KtFile
        val model = analyze(file, "Empty")

        assertEquals("Empty", model.name)
        assertTrue(model.fields.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Nested messages
    // -------------------------------------------------------------------------

    fun `test wires a nested data class as a nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val address: Address)") as KtFile
        val field = analyze(file, "User").fields[0]

        assertEquals("Address", (field.fieldType as ProtoFieldType.MessageRef).typeName)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    fun `test wires a three-level hierarchy`() {
        myFixture.addFileToProject("Coordinates.kt", "package com.example\ndata class Coordinates(val lat: Double)")
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val coordinates: Coordinates)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val address: Address)") as KtFile
        val address = analyze(file, "User").fields[0].nestedMessage!!

        assertEquals("Coordinates", address.fields[0].nestedMessage!!.name)
    }

    fun `test shares one nested message instance for a class referenced twice`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val home: Address, val work: Address)") as KtFile
        val model = analyze(file, "User")

        assertSame(model.fields[0].nestedMessage, model.fields[1].nestedMessage)
    }

    fun `test terminates on a cyclic reference`() {
        myFixture.addFileToProject("B.kt", "package com.example\ndata class B(val value: String)")
        val file = myFixture.addFileToProject("A.kt", "package com.example\ndata class A(val b: B)") as KtFile
        val model = analyze(file, "A")

        val names = generateSequence(model) { it.fields.firstOrNull()?.nestedMessage }.map { it.name }.toList()
        assertEquals(names.distinct(), names)
    }

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    fun `test wires an enum with its entries`() {
        myFixture.addFileToProject("Score.kt", "package com.example\nenum class Score { HIGH, LOW, MEDIUM }")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val score: Score)") as KtFile
        val field = analyze(file, "User").fields[0]

        assertEquals("Score", (field.fieldType as ProtoFieldType.EnumRef).typeName)
        assertEquals(linkedSetOf("HIGH", "LOW", "MEDIUM"), field.nestedEnum!!.entries)
    }

    // -------------------------------------------------------------------------
    // Integration
    // -------------------------------------------------------------------------

    fun `test wires a type alias through to a nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.example\ntypealias MyAddress = Address\ndata class User(val address: MyAddress)",
            ) as KtFile
        val field = analyze(file, "User").fields[0]

        assertEquals("Address", (field.fieldType as ProtoFieldType.MessageRef).typeName)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    private fun analyze(
        file: KtFile,
        className: String,
    ): ProtoMessage = inSmartReadAction { K2ClassAnalyzer().analyze(file.findClass(className)) }
}
