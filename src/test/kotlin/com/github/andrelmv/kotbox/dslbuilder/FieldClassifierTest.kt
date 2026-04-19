package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.BuilderField
import com.github.andrelmv.kotbox.dslbuilder.generator.FieldClassifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass

class FieldClassifierTest : BasePlatformTestCase() {
    fun `test classifies String field as Simple required`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)")
        val cls = getClass("User")
        val field = classify(cls, 0)
        assertTrue(field is BuilderField.Simple)
        field as BuilderField.Simple
        assertEquals("name", field.name)
        assertTrue(field.isRequired)
    }

    fun `test classifies nullable field as not required`() {
        myFixture.configureByText("Test.kt", "data class User(val nick: String?)")
        assertFalse(classify(getClass("User"), 0).isRequired)
    }

    fun `test classifies field with default as not required`() {
        myFixture.configureByText("Test.kt", "data class User(val active: Boolean = true)")
        assertFalse(classify(getClass("User"), 0).isRequired)
    }

    fun `test classifies data class field as NestedBuilder`() {
        myFixture.configureByText(
            "Test.kt",
            """
            data class Address(val street: String)
            data class User(val address: Address)
            """.trimIndent(),
        )
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.NestedBuilder)
        field as BuilderField.NestedBuilder
        assertEquals("AddressBuilder", field.builderTypeName)
    }

    fun `test classifies List of String as SimpleList`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: List<String>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.SimpleList)
        assertEquals("String", (field as BuilderField.SimpleList).elementTypeName)
    }

    fun `test classifies List of data class as NestedBuilderList`() {
        myFixture.configureByText(
            "Test.kt",
            """
            data class Role(val name: String)
            data class User(val roles: List<Role>)
            """.trimIndent(),
        )
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.NestedBuilderList)
        assertEquals("RoleBuilder", (field as BuilderField.NestedBuilderList).elementBuilderTypeName)
    }

    fun `test classifies Set field as SimpleSet`() {
        myFixture.configureByText("Test.kt", "data class User(val codes: Set<String>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.SimpleSet)
        assertEquals("String", (field as BuilderField.SimpleSet).elementTypeName)
    }

    fun `test classifies Map field correctly`() {
        myFixture.configureByText("Test.kt", "data class User(val meta: Map<String, Int>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.SimpleMap)
        field as BuilderField.SimpleMap
        assertEquals("String", field.keyTypeName)
        assertEquals("Int", field.valueTypeName)
    }

    fun `test classifies MutableList field as SimpleList`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: MutableList<String>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.SimpleList)
        assertEquals("String", (field as BuilderField.SimpleList).elementTypeName)
    }

    fun `test classifies MutableSet field as SimpleSet`() {
        myFixture.configureByText("Test.kt", "data class User(val codes: MutableSet<Int>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.SimpleSet)
        assertEquals("Int", (field as BuilderField.SimpleSet).elementTypeName)
    }

    fun `test classifies MutableMap field as SimpleMap`() {
        myFixture.configureByText("Test.kt", "data class User(val meta: MutableMap<String, Any>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is BuilderField.SimpleMap)
        field as BuilderField.SimpleMap
        assertEquals("String", field.keyTypeName)
        assertEquals("Any", field.valueTypeName)
    }

    fun `test classifies nullable data class field as NestedBuilder`() {
        myFixture.configureByText(
            "Test.kt",
            """
            data class Address(val street: String)
            data class User(val address: Address?)
            """.trimIndent(),
        )
        val field = classify(getClass("User"), 0)
        // nullable data class must still be classified as NestedBuilder (not Simple)
        assertTrue(field is BuilderField.NestedBuilder)
        assertEquals("AddressBuilder", (field as BuilderField.NestedBuilder).builderTypeName)
    }

    fun `test classifies nullable List of data class as NestedBuilderList`() {
        myFixture.configureByText(
            "Test.kt",
            """
            data class Role(val name: String)
            data class User(val roles: List<Role>?)
            """.trimIndent(),
        )
        val field = classify(getClass("User"), 0)
        // nullable List<data class> must still be classified as NestedBuilderList
        assertTrue(field is BuilderField.NestedBuilderList)
        assertEquals("RoleBuilder", (field as BuilderField.NestedBuilderList).elementBuilderTypeName)
    }

    // Helpers
    private fun getClass(name: String) =
        myFixture.file.children
            .filterIsInstance<KtClass>()
            .first { it.name == name }

    private fun classify(
        cls: KtClass,
        paramIndex: Int,
    ): BuilderField {
        val classifier = FieldClassifier(project, GlobalSearchScope.projectScope(project), cls)
        return classifier.classify(cls.primaryConstructorParameters[paramIndex])!!
    }
}
