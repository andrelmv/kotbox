package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.placement.SameFilePlacement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile

class SameFilePlacementTest : BasePlatformTestCase() {
    fun `test insert appends annotation class to file`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)\n")
        val file = myFixture.file as KtFile

        SameFilePlacement.insert(file, "@DslMarker\nannotation class UserDsl", project)

        assertTrue(file.text.contains("annotation class UserDsl"))
    }

    fun `test insert appends builder class to file`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)\n")
        val file = myFixture.file as KtFile

        SameFilePlacement.insert(
            file,
            "@UserDsl\nclass UserBuilder {\n    var name: String? = null\n    fun build(): User = User(name = name ?: error(\"\"))\n}",
            project,
        )

        assertTrue(file.text.contains("class UserBuilder"))
        assertTrue(file.text.contains("fun build(): User"))
    }

    fun `test insert preserves original declarations`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)\n")
        val file = myFixture.file as KtFile

        SameFilePlacement.insert(file, "class UserBuilder {}", project)

        assertTrue(file.text.contains("data class User"))
        assertTrue(file.text.contains("class UserBuilder"))
    }

    fun `test insert handles multiple declarations in generated code`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String)\n")
        val file = myFixture.file as KtFile

        SameFilePlacement.insert(
            file,
            "@DslMarker\nannotation class UserDsl\n\nclass UserBuilder {}",
            project,
        )

        assertTrue(file.text.contains("annotation class UserDsl"))
        assertTrue(file.text.contains("class UserBuilder"))
    }
}
