package com.github.andrelmv.kotbox.proto.generator

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

internal class KotlinClassFinderTest : BasePlatformTestCase() {
    fun `test finds data class from explicit import over same-named class in different package`() {
        myFixture.addFileToProject(
            "com/example/dto/Address.kt",
            "package com.example.dto\ndata class Address(val street: String)",
        )
        myFixture.addFileToProject(
            "com/example/model/Address.kt",
            "package com.example.model\ndata class Address(val city: String)",
        )
        val file =
            myFixture.addFileToProject(
                "User.kt",
                """
                package com.example
                import com.example.dto.Address
                data class User(val address: Address)
                """.trimIndent(),
            ) as KtFile

        val typeRef = typeReferenceOf(file, "Address")
        val result = finder().findDataClass("Address", typeRef)

        assertNotNull(result)
        assertEquals("com.example.dto.Address", result!!.fqName?.asString())
        assertEquals("street", result.primaryConstructorParameters[0].name)
    }

    fun `test finds enum class from explicit import over same-named class in different package`() {
        myFixture.addFileToProject(
            "com/example/dto/Score.kt",
            "package com.example.dto\nenum class Score { HIGH, LOW }",
        )
        myFixture.addFileToProject(
            "com/example/model/Score.kt",
            "package com.example.model\nenum class Score { A, B, C }",
        )
        val file =
            myFixture.addFileToProject(
                "User.kt",
                """
                package com.example
                import com.example.dto.Score
                data class User(val score: Score)
                """.trimIndent(),
            ) as KtFile

        val typeRef = typeReferenceOf(file, "Score")
        val result = finder().findEnumClass("Score", typeRef)

        assertNotNull(result)
        assertEquals("com.example.dto.Score", result!!.fqName?.asString())
    }

    fun `test finds data class from same package when no explicit import`() {
        myFixture.addFileToProject(
            "com/example/Address.kt",
            "package com.example\ndata class Address(val street: String)",
        )
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val address: Address)",
            ) as KtFile

        val typeRef = typeReferenceOf(file, "Address")
        val result = finder().findDataClass("Address", typeRef)

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    fun `test finds same-package class over other-package class when no import`() {
        myFixture.addFileToProject(
            "com/example/Address.kt",
            "package com.example\ndata class Address(val street: String)",
        )
        myFixture.addFileToProject(
            "com/other/Address.kt",
            "package com.other\ndata class Address(val city: String)",
        )
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val address: Address)",
            ) as KtFile

        val typeRef = typeReferenceOf(file, "Address")
        val result = finder().findDataClass("Address", typeRef)

        assertNotNull(result)
        assertEquals("street", result!!.primaryConstructorParameters[0].name)
    }

    fun `test finds enum class from same package when no explicit import`() {
        myFixture.addFileToProject(
            "com/example/Score.kt",
            "package com.example\nenum class Score { HIGH, LOW, MEDIUM }",
        )
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val score: Score)",
            ) as KtFile

        val typeRef = typeReferenceOf(file, "Score")
        val result = finder().findEnumClass("Score", typeRef)

        assertNotNull(result)
        assertEquals("com.example.Score", result!!.fqName?.asString())
    }

    fun `test does not return regular class from findDataClass`() {
        myFixture.addFileToProject(
            "Foo.kt",
            "package com.example\nclass Foo(val x: String)",
        )
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.example\ndata class User(val foo: Foo)",
            ) as KtFile

        val typeRef = typeReferenceOf(file, "Foo")
        val result = finder().findDataClass("Foo", typeRef)

        assertNull(result)
    }

    fun `test returns null for unknown type with no import and no same-package match`() {
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.example\ndata class User(val ghost: GhostType)",
            ) as KtFile

        val typeRef = typeReferenceOf(file, "GhostType")
        val result = finder().findDataClass("GhostType", typeRef)

        assertNull(result)
    }

    fun `test returns null for enum lookup of unknown type`() {
        val file =
            myFixture.addFileToProject(
                "User.kt",
                "package com.example\ndata class User(val ghost: GhostType)",
            ) as KtFile

        val typeRef = typeReferenceOf(file, "GhostType")
        val result = finder().findEnumClass("GhostType", typeRef)

        assertNull(result)
    }

    private fun finder() = KotlinClassFinder(project, GlobalSearchScope.projectScope(project))

    /**
     * Finds the first type reference in [file] whose text matches [typeName].
     * Used to provide a realistic [org.jetbrains.kotlin.psi.KtTypeReference] with import context for testing.
     */
    private fun typeReferenceOf(
        file: KtFile,
        typeName: String,
    ) = file.children
        .filterIsInstance<KtClass>()
        .flatMap { it.primaryConstructorParameters }
        .mapNotNull { it.typeReference }
        .first { it.text.trimEnd('?') == typeName }
}
