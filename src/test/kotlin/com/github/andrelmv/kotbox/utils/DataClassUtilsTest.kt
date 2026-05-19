package com.github.andrelmv.kotbox.utils

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

    // Helpers
    private fun getClass(name: String) =
        myFixture.file.children
            .filterIsInstance<KtClass>()
            .first { it.name == name }
}
