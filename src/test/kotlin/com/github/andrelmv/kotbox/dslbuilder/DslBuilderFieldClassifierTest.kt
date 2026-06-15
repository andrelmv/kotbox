package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.DslBuilderField
import com.github.andrelmv.kotbox.dslbuilder.generator.DslBuilderFieldClassifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass

class DslBuilderFieldClassifierTest : BasePlatformTestCase() {
    fun `test classifies String field as Simple required`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)")
        val cls = getClass("User")
        val field = classify(cls, 0)
        assertTrue(field is DslBuilderField.Simple)
        field as DslBuilderField.Simple
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
        assertTrue(field is DslBuilderField.NestedBuilder)
        field as DslBuilderField.NestedBuilder
        assertEquals("AddressBuilder", field.builderTypeName)
    }

    fun `test classifies List of String as SimpleList`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: List<String>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is DslBuilderField.SimpleList)
        assertEquals("String", (field as DslBuilderField.SimpleList).elementTypeName)
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
        assertTrue(field is DslBuilderField.NestedBuilderList)
        assertEquals("RoleBuilder", (field as DslBuilderField.NestedBuilderList).elementBuilderTypeName)
    }

    fun `test classifies Set field as SimpleSet`() {
        myFixture.configureByText("Test.kt", "data class User(val codes: Set<String>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is DslBuilderField.SimpleSet)
        assertEquals("String", (field as DslBuilderField.SimpleSet).elementTypeName)
    }

    fun `test classifies Map field correctly`() {
        myFixture.configureByText("Test.kt", "data class User(val meta: Map<String, Int>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is DslBuilderField.SimpleMap)
        field as DslBuilderField.SimpleMap
        assertEquals("String", field.keyTypeName)
        assertEquals("Int", field.valueTypeName)
    }

    fun `test classifies MutableList field as SimpleList`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: MutableList<String>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is DslBuilderField.SimpleList)
        assertEquals("String", (field as DslBuilderField.SimpleList).elementTypeName)
    }

    fun `test classifies MutableSet field as SimpleSet`() {
        myFixture.configureByText("Test.kt", "data class User(val codes: MutableSet<Int>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is DslBuilderField.SimpleSet)
        assertEquals("Int", (field as DslBuilderField.SimpleSet).elementTypeName)
    }

    fun `test classifies MutableMap field as SimpleMap`() {
        myFixture.configureByText("Test.kt", "data class User(val meta: MutableMap<String, Any>)")
        val field = classify(getClass("User"), 0)
        assertTrue(field is DslBuilderField.SimpleMap)
        field as DslBuilderField.SimpleMap
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
        assertTrue(field is DslBuilderField.NestedBuilder)
        assertEquals("AddressBuilder", (field as DslBuilderField.NestedBuilder).builderTypeName)
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
        assertTrue(field is DslBuilderField.NestedBuilderList)
        assertEquals("RoleBuilder", (field as DslBuilderField.NestedBuilderList).elementBuilderTypeName)
    }

    fun `test classifies Class star field preserves full generic type`() {
        myFixture.configureByText("Test.kt", "data class Event(val type: Class<*>)")
        val field = classify(getClass("Event"), 0)
        assertTrue(field is DslBuilderField.Simple)
        assertEquals("Class<*>", (field as DslBuilderField.Simple).typeName)
    }

    fun `test classifies generic non-data-class field preserves full generic type`() {
        myFixture.configureByText("Test.kt", "data class Event(val handler: Comparable<String>)")
        val field = classify(getClass("Event"), 0)
        assertTrue(field is DslBuilderField.Simple)
        assertEquals("Comparable<String>", (field as DslBuilderField.Simple).typeName)
    }

    // Helpers
    private fun getClass(name: String) =
        myFixture.file.children
            .filterIsInstance<KtClass>()
            .first { it.name == name }

    private fun classify(
        cls: KtClass,
        paramIndex: Int,
    ): DslBuilderField {
        val classifier = DslBuilderFieldClassifier(project, GlobalSearchScope.projectScope(project), cls)
        return classifier.classify(cls.primaryConstructorParameters[paramIndex])!!
    }
}
