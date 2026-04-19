package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.placement.NewFilePlacement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile

class NewFilePlacementTest : BasePlatformTestCase() {
    fun `test insert creates new kt file in the same directory`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "UserBuilder", "class UserBuilder {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("UserBuilder.kt"))
    }

    fun `test insert adds kt extension when missing`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "UserBuilder", "class UserBuilder {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("UserBuilder.kt"))
    }

    fun `test insert does not double-add kt extension`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "UserBuilder.kt", "class UserBuilder {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("UserBuilder.kt"))
        // Ensure "UserBuilder.kt.kt" was not created
        assertNull(sourceFile.containingDirectory?.findFile("UserBuilder.kt.kt"))
    }

    fun `test insert prepends package declaration from source file`() {
        myFixture.configureByText("User.kt", "package com.example\ndata class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "UserBuilder", "class UserBuilder {}", project)

        val newFile = sourceFile.containingDirectory?.findFile("UserBuilder.kt") as? KtFile
        assertEquals("com.example", newFile?.packageFqName?.asString())
    }

    fun `test insert works without a package declaration`() {
        myFixture.configureByText("User.kt", "data class User(val name: String)")
        val sourceFile = myFixture.file as KtFile

        NewFilePlacement.insert(sourceFile, "UserBuilder", "class UserBuilder {}", project)

        assertNotNull(sourceFile.containingDirectory?.findFile("UserBuilder.kt"))
    }
}
