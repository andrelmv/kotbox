package com.github.andrelmv.kotbox.proto.generator

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass

internal class ProtoAnalyzerTest : BasePlatformTestCase() {
    fun `test analyzes single data class with scalar fields`() {
        myFixture.configureByText("User.kt", "data class User(val name: String, val age: Int)")
        val model = analyze("User")
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
        assertTrue(age.fieldType is ProtoFieldType.Scalar)
        assertEquals("int32", (age.fieldType as ProtoFieldType.Scalar).protoType)
    }

    fun `test analyzes nested data class`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val name: String, val address: Address)")
        val model = analyze("User")
        assertEquals(2, model.fields.size)

        val addressField = model.fields[1]
        assertEquals("address", addressField.name)
        assertTrue(addressField.fieldType is ProtoFieldType.MessageRef)
        assertEquals("Address", (addressField.fieldType as ProtoFieldType.MessageRef).typeName)
        assertNotNull(addressField.nestedMessage)
        assertEquals("Address", addressField.nestedMessage!!.name)
    }

    fun `test analyzes three-level hierarchy`() {
        myFixture.addFileToProject("Coords.kt", "package com.ex\ndata class Coordinates(val lat: Double, val lng: Double)")
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String, val coordinates: Coordinates)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val name: String, val address: Address)")
        val model = analyze("User")
        val addressField = model.fields[1]
        assertNotNull(addressField.nestedMessage)

        val coordsField = addressField.nestedMessage!!.fields[1]
        assertEquals("coordinates", coordsField.name)
        assertNotNull(coordsField.nestedMessage)
        assertEquals("Coordinates", coordsField.nestedMessage!!.name)
    }

    fun `test nullable scalar field produces optional modifier`() {
        myFixture.configureByText("User.kt", "data class User(val nickname: String?)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Scalar)
        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.Scalar).modifier)
    }

    fun `test nullable nested data class produces optional modifier`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val address: Address?)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.MessageRef)
        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.MessageRef).modifier)
        assertNotNull(field.nestedMessage)
    }

    fun `test List of scalar produces Repeated field`() {
        myFixture.configureByText("User.kt", "data class User(val tags: List<String>)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Repeated)
        assertEquals("string", (field.fieldType as ProtoFieldType.Repeated).elementProto)
        assertNull(field.nestedMessage)
    }

    fun `test List of data class produces Repeated field with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val addresses: List<Address>)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Repeated)
        assertEquals("Address", (field.fieldType as ProtoFieldType.Repeated).elementProto)
        assertNotNull(field.nestedMessage)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    fun `test Set of scalar produces Repeated field`() {
        myFixture.configureByText("User.kt", "data class User(val tags: Set<String>)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Repeated)
        assertEquals("string", (field.fieldType as ProtoFieldType.Repeated).elementProto)
    }

    fun `test Map of scalar key and scalar value produces Map field`() {
        myFixture.configureByText("User.kt", "data class User(val scores: Map<String, Int>)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Map)
        val mapType = field.fieldType as ProtoFieldType.Map
        assertEquals("string", mapType.keyProto)
        assertEquals("int32", mapType.valueProto)
        assertNull(field.nestedMessage)
    }

    fun `test Map of scalar key and data class value produces Map field with nested message`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val addresses: Map<String, Address>)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Map)
        val mapType = field.fieldType as ProtoFieldType.Map
        assertEquals("string", mapType.keyProto)
        assertEquals("Address", mapType.valueProto)
        assertNotNull(field.nestedMessage)
        assertEquals("Address", field.nestedMessage!!.name)
    }

    fun `test enum field produces Scalar field with nested enum`() {
        myFixture.addFileToProject("Score.kt", "package com.ex\nenum class Score { HIGH, LOW, MEDIUM }")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val score: Score)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.EnumRef)
        assertEquals("Score", (field.fieldType as ProtoFieldType.EnumRef).typeName)
        assertNotNull(field.nestedEnum)
        assertEquals("Score", field.nestedEnum!!.name)
        assertEquals(setOf("HIGH", "LOW", "MEDIUM"), field.nestedEnum.entries)
    }

    fun `test nullable enum field produces optional modifier`() {
        myFixture.addFileToProject("Score.kt", "package com.ex\nenum class Score { HIGH, LOW }")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val score: Score?)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.EnumRef)
        assertEquals(ProtoModifier.OPTIONAL, (field.fieldType as ProtoFieldType.EnumRef).modifier)
        assertNotNull(field.nestedEnum)
    }

    fun `test unresolvable type produces unresolved field`() {
        myFixture.configureByText("User.kt", "data class User(val unknown: SomeExternalType)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.unresolved)
        assertEquals("SomeExternalType", (field.fieldType as ProtoFieldType.MessageRef).typeName)
    }

    fun `test class with no fields produces empty field list`() {
        myFixture.configureByText("Empty.kt", "data class Empty()")
        val model = analyze("Empty")
        assertEquals("Empty", model.name)
        assertTrue(model.fields.isEmpty())
    }

    fun `test field numbers are assigned sequentially from 1`() {
        myFixture.configureByText("User.kt", "data class User(val a: String, val b: Int, val c: Boolean)")
        val model = analyze("User")
        assertEquals(1, model.fields[0].number)
        assertEquals(2, model.fields[1].number)
        assertEquals(3, model.fields[2].number)
    }

    fun `test does not infinite loop on cyclic reference`() {
        // A references B, B references A — currentlyProcessing guard must prevent infinite recursion
        myFixture.addFileToProject("B.kt", "data class B(val value: String)")
        myFixture.configureByText("A.kt", "data class A(val b: B)")
        val model = analyze("A")
        val classNames =
            generateSequence(model) { it.fields.firstOrNull()?.nestedMessage }
                .map { it.name }
                .toList()
        assertEquals(classNames.distinct(), classNames)
    }

    fun `test camelCase field name is preserved in model`() {
        myFixture.configureByText("User.kt", "data class User(val firstName: String)")
        val model = analyze("User")
        assertEquals("firstName", model.fields[0].name)
    }

    fun `test same data class referenced in two fields produces same instance`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val home: Address, val work: Address)")
        val model = analyze("User")
        val home = model.fields[0].nestedMessage
        val work = model.fields[1].nestedMessage
        assertNotNull(home)
        assertNotNull(work)
        assertTrue(home === work)
    }

    fun `test nullable List produces Repeated field`() {
        myFixture.configureByText("User.kt", "data class User(val tags: List<String>?)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Repeated)
    }

    fun `test nullable Map produces Map field`() {
        myFixture.configureByText("User.kt", "data class User(val scores: Map<String, Int>?)")
        val model = analyze("User")
        val field = model.fields[0]
        assertTrue(field.fieldType is ProtoFieldType.Map)
    }

    private fun analyze(className: String): ProtoMessage {
        val cls =
            myFixture.file.children
                .filterIsInstance<KtClass>()
                .firstOrNull { it.name == className }
                ?: error("Class '$className' not found")
        return ProtoAnalyzer(project, GlobalSearchScope.projectScope(project)).analyze(cls)
    }
}
