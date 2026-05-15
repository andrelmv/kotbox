package com.github.andrelmv.kotbox.utils

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
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

    fun `test returns data class when caret inside data class`() {
        myFixture.configureByText(
            "Test.kt",
            """
            data class User(
                val na<caret>me: String,
            )
            """.trimIndent(),
        )

        val result = getDataClass(createEvent())

        assertNotNull(result)
        assertEquals("User", result?.name)
    }

    fun `test returns null when caret inside regular class`() {
        myFixture.configureByText(
            "Test.kt",
            """
            class Us<caret>er(
                val name: String,
            )
            """.trimIndent(),
        )

        val result = getDataClass(createEvent())

        assertNull(result)
    }

    private fun getClass(name: String) =
        myFixture.file
            .children
            .filterIsInstance<KtClass>()
            .first { it.name == name }

    private fun createEvent(): AnActionEvent {
        val dataContext =
            DataContext { dataId ->
                when (dataId) {
                    CommonDataKeys.EDITOR.name -> myFixture.editor
                    CommonDataKeys.PSI_FILE.name -> myFixture.file
                    else -> null
                }
            }

        return AnActionEvent.createEvent(
            dataContext,
            Presentation(),
            "test",
            ActionUiKind.NONE,
            null,
        )
    }
}
