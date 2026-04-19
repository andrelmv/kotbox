package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.analyzeK2
import com.github.andrelmv.kotbox.dslbuilder.generator.isDataClass
import com.github.andrelmv.kotbox.dslbuilder.generator.simpleTypeName
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass

class DataClassUtilsTest : BasePlatformTestCase() {
    fun `test isDataClass returns true for data class`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)")
        assertTrue(getClass("User").isDataClass())
    }

    fun `test isDataClass returns false for regular class`() {
        myFixture.configureByText("Test.kt", "class User(val name: String)")
        assertFalse(getClass("User").isDataClass())
    }

    // simpleTypeName

    fun `test simpleTypeName returns plain type name`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertEquals("String", param.simpleTypeName())
    }

    fun `test simpleTypeName strips nullable marker`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String?)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertEquals("String", param.simpleTypeName())
    }

    fun `test simpleTypeName strips generic type arguments`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: List<String>)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertEquals("List", param.simpleTypeName())
    }

    fun `test simpleTypeName strips both generics and nullable marker`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: List<String>?)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertEquals("List", param.simpleTypeName())
    }

    fun `test simpleTypeName handles Map type`() {
        myFixture.configureByText("Test.kt", "data class User(val meta: Map<String, Int>)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertEquals("Map", param.simpleTypeName())
    }

    // analyzeK2

    fun `test analyzeK2 non-nullable non-default is required`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)")
        val param = getClass("User").primaryConstructorParameters.first()
        val result = param.analyzeK2()
        assertFalse(result.isNullable)
        assertFalse(result.hasDefault)
    }

    fun `test analyzeK2 detects nullable type`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String?)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertTrue(param.analyzeK2().isNullable)
    }

    fun `test analyzeK2 detects default value`() {
        myFixture.configureByText("Test.kt", "data class User(val active: Boolean = true)")
        val param = getClass("User").primaryConstructorParameters.first()
        assertTrue(param.analyzeK2().hasDefault)
    }

    // Helpers

    private fun getClass(name: String) =
        myFixture.file.children
            .filterIsInstance<KtClass>()
            .first { it.name == name }
}
