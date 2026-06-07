package com.github.andrelmv.kotbox.proto.dialog

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile

internal class NewFilePlacementTest : BasePlatformTestCase() {
    fun `test insert creates new file in the same directory`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "User", "message User {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("User.proto"))
    }

    fun `test insert adds proto extension when missing`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "User", "message User {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("User.proto"))
    }

    fun `test insert does not double-add proto extension`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "User.proto", "message User {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("User.proto"))
        assertNull(sourceFile.containingDirectory?.findFile("User.proto.proto"))
    }

    fun `test insert writes the generated content`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "User", "message User {}", project)

        val newFile = sourceFile.containingDirectory?.findFile("User.proto")
        assertEquals("message User {}", newFile?.text)
    }
}
