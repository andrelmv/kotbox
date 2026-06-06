package com.github.andrelmv.kotbox.proto.generator

import com.intellij.openapi.application.ReadAction
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.Callable

internal class K2ClassAnalyzerTest : BasePlatformTestCase() {
    override fun runInDispatchThread(): Boolean = false

    // -------------------------------------------------------------------------
    // Scalar / field-level building
    // -------------------------------------------------------------------------

    fun `test analyzes single data class with scalar fields`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val name: String, val age: Int)") as KtFile
        val model = analyze(file, "User")
        assertEquals("User", model.name)
        assertEquals(2, model.fields.size)

        val name = model.fields[0]
        assertEquals("name", name.name)
        assertEquals(1, name.number)
        assertTrue(name.fieldType is ProtoFieldType.Scalar)
        assertEquals("string", (name.fieldType as ProtoFieldType.Scalar).protoType)

        val age = model.fields[1]
        assertEquals("age", age.name)
        assertEquals(2, age.number)
        assertEquals("int32", (age.fieldType as ProtoFieldType.Scalar).protoType)
    }

    fun `test nullable scalar field produces optional modifier`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val nickname: String?)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Scalar)
        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.Scalar).modifier)
    }

    fun `test field numbers are assigned sequentially from 1`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val a: String, val b: Int, val c: Boolean)") as KtFile
        val model = analyze(file, "User")
        assertEquals(listOf(1, 2, 3), model.fields.map { it.number })
    }

    fun `test camelCase field name is preserved in model`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val firstName: String)") as KtFile
        assertEquals("firstName", analyze(file, "User").fields[0].name)
    }

    fun `test class with no fields produces empty field list`() {
        val file = myFixture.addFileToProject("Empty.kt", "data class Empty()") as KtFile
        val model = analyze(file, "Empty")
        assertEquals("Empty", model.name)
        assertTrue(model.fields.isEmpty())
    }

    fun `test unresolvable type produces unresolved field`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val unknown: SomeExternalType)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertTrue(field.unresolved)
        assertEquals("SomeExternalType", (field.fieldType as ProtoFieldType.MessageRef).typeName)
    }

    // -------------------------------------------------------------------------
    // Nested messages
    // -------------------------------------------------------------------------

    fun `test analyzes nested data class`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val name: String, val address: Address)") as KtFile
        val model = analyze(file, "User")
        assertEquals(2, model.fields.size)

        val addressField = model.fields[1]
        assertEquals("address", addressField.name)
        assertTrue(addressField.fieldType is ProtoFieldType.MessageRef)
        assertEquals("Address", (addressField.fieldType as ProtoFieldType.MessageRef).typeName)
        assertEquals("Address", addressField.nestedMessage!!.name)
    }

    fun `test analyzes three-level hierarchy`() {
        myFixture.addFileToProject("Coords.kt", "package com.ex\ndata class Coordinates(val lat: Double, val lng: Double)")
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String, val coordinates: Coordinates)")
        val file =
            myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val name: String, val address: Address)") as KtFile
        val addressField = analyze(file, "User").fields[1]
        assertNotNull(addressField.nestedMessage)

        val coordsField = addressField.nestedMessage!!.fields[1]
        assertEquals("coordinates", coordsField.name)
        assertEquals("Coordinates", coordsField.nestedMessage!!.name)
    }

    fun `test nullable nested data class produces optional modifier`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val address: Address?)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertTrue(field.fieldType is ProtoFieldType.MessageRef)
        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.MessageRef).modifier)
        assertNotNull(field.nestedMessage)
    }

    fun `test same data class referenced in two fields produces same instance`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val home: Address, val work: Address)") as KtFile
        val model = analyze(file, "User")
        val home = model.fields[0].nestedMessage
        val work = model.fields[1].nestedMessage
        assertNotNull(home)
        assertTrue(home === work)
    }

    fun `test does not infinite loop on cyclic reference`() {
        myFixture.addFileToProject("B.kt", "data class B(val value: String)")
        val file = myFixture.addFileToProject("A.kt", "data class A(val b: B)") as KtFile
        val model = analyze(file, "A")
        val classNames =
            generateSequence(model) { it.fields.firstOrNull()?.nestedMessage }
                .map { it.name }
                .toList()
        assertEquals(classNames.distinct(), classNames)
    }

    // -------------------------------------------------------------------------
    // Collections & maps
    // -------------------------------------------------------------------------

    fun `test List of scalar produces Repeated field`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val tags: List<String>)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("string", (field.fieldType as ProtoFieldType.Repeated).elementProto)
        assertNull(field.nestedMessage)
    }

    fun `test Set of scalar produces Repeated field`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val tags: Set<String>)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("string", (field.fieldType as ProtoFieldType.Repeated).elementProto)
    }

    fun `test nullable List produces Repeated field`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val tags: List<String>?)") as KtFile
        assertTrue(analyze(file, "User").fields[0].fieldType is ProtoFieldType.Repeated)
    }

    fun `test List of data class produces Repeated field with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val addresses: List<Address>)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("Address", (field.fieldType as ProtoFieldType.Repeated).elementProto)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    fun `test Map of scalar key and scalar value produces Map field`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val scores: Map<String, Int>)") as KtFile
        val mapType = analyze(file, "User").fields[0].fieldType as ProtoFieldType.Map
        assertEquals("string", mapType.keyProto)
        assertEquals("int32", mapType.valueProto)
    }

    fun `test nullable Map produces Map field`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val scores: Map<String, Int>?)") as KtFile
        assertTrue(analyze(file, "User").fields[0].fieldType is ProtoFieldType.Map)
    }

    fun `test Map of scalar key and data class value produces Map field with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val addresses: Map<String, Address>)") as KtFile
        val field = analyze(file, "User").fields[0]
        val mapType = field.fieldType as ProtoFieldType.Map
        assertEquals("Address", mapType.valueProto)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    fun `test enum field produces EnumRef with nested enum`() {
        myFixture.addFileToProject("Score.kt", "package com.ex\nenum class Score { HIGH, LOW, MEDIUM }")
        val file = myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val score: Score)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("Score", (field.fieldType as ProtoFieldType.EnumRef).typeName)
        assertEquals("Score", field.nestedEnum!!.name)
        assertEquals(linkedSetOf("HIGH", "LOW", "MEDIUM"), field.nestedEnum.entries)
    }

    fun `test nullable enum field produces optional modifier`() {
        myFixture.addFileToProject("Score.kt", "package com.ex\nenum class Score { HIGH, LOW }")
        val file = myFixture.addFileToProject("User.kt", "package com.ex\ndata class User(val score: Score?)") as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.EnumRef).modifier)
        assertNotNull(field.nestedEnum)
    }

    // -------------------------------------------------------------------------
    // Type-alias integration (alias → nested message/enum wiring through the full pipeline)
    // -------------------------------------------------------------------------

    fun `test typealias for data class resolves to message ref with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.ex\ntypealias MyAddress = Address\ndata class User(val address: MyAddress)",
            ) as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("Address", (field.fieldType as ProtoFieldType.MessageRef).typeName)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    fun `test typealias for List of data class resolves to repeated field with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.ex\ntypealias AddressList = List<Address>\ndata class User(val addresses: AddressList)",
            ) as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("Address", (field.fieldType as ProtoFieldType.Repeated).elementProto)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    fun `test typealias for Map with data class value resolves to map field with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.ex\ntypealias AddressMap = Map<String, Address>\ndata class User(val addresses: AddressMap)",
            ) as KtFile
        val field = analyze(file, "User").fields[0]
        assertEquals("Address", (field.fieldType as ProtoFieldType.Map).valueProto)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private fun analyze(
        file: KtFile,
        className: String,
    ): ProtoMessage {
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return ReadAction
            .nonBlocking(
                Callable {
                    val cls =
                        file.children
                            .filterIsInstance<KtClass>()
                            .firstOrNull { it.name == className }
                            ?: error("Class '$className' not found")
                    K2ClassAnalyzer().analyze(cls)
                },
            ).inSmartMode(project)
            .executeSynchronously()
    }
}
