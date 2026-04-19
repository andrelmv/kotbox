package com.github.andrelmv.kotbox.dslbuilder

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DslBuilderIntentionTest : BasePlatformTestCase() {
    private val intention = DslBuilderIntention()

    fun `test text and familyName`() {
        assertEquals("Generate DSL builder", intention.text)
        assertEquals("Kotlin Toolbox", intention.familyName)
    }

    fun `test startInWriteAction returns false`() {
        assertFalse(intention.startInWriteAction())
    }

    fun `test isAvailable returns true when caret is inside a data class`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String<caret>)")
        assertTrue(intention.isAvailable(project, myFixture.editor, myFixture.file))
    }

    fun `test isAvailable returns false for regular class`() {
        myFixture.configureByText("Test.kt", "class User(val name: String<caret>)")
        assertFalse(intention.isAvailable(project, myFixture.editor, myFixture.file))
    }

    fun `test isAvailable returns false when caret is outside any class`() {
        myFixture.configureByText("Test.kt", "val x = 1<caret>")
        assertFalse(intention.isAvailable(project, myFixture.editor, myFixture.file))
    }

    fun `test isAvailable returns false when editor is null`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)")
        assertFalse(intention.isAvailable(project, null, myFixture.file))
    }

    fun `test isAvailable returns false for non-Kotlin file`() {
        myFixture.configureByText("Test.java", "public class User {}")
        assertFalse(intention.isAvailable(project, myFixture.editor, myFixture.file))
    }
}
