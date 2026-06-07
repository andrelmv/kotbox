package com.github.andrelmv.kotbox.proto.generator

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

internal class KtClassResolverTest : ProtoGeneratorTestCase() {
    // -------------------------------------------------------------------------
    // Direct type resolution (mapped = null)
    // -------------------------------------------------------------------------

    fun `test resolves data class from explicit import over same-named class in different package`() {
        myFixture.addFileToProject("com/example/dto/Address.kt", "package com.example.dto\ndata class Address(val street: String)")
        myFixture.addFileToProject("com/example/model/Address.kt", "package com.example.model\ndata class Address(val city: String)")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                """
                package com.example
                import com.example.dto.Address
                data class User(val address: Address)
                """.trimIndent(),
            ) as KtFile

        val result = findClassForType(file, "Address")

        assertNotNull(result)
        assertEquals("com.example.dto.Address", result!!.fqName?.asString())
        assertEquals("street", result.primaryConstructorParameters[0].name)
    }

    fun `test resolves enum class from explicit import over same-named class in different package`() {
        myFixture.addFileToProject("com/example/dto/Score.kt", "package com.example.dto\nenum class Score { HIGH, LOW }")
        myFixture.addFileToProject("com/example/model/Score.kt", "package com.example.model\nenum class Score { A, B, C }")
        val file =
            myFixture.addFileToProject(
                "User.kt",
                """
                package com.example
                import com.example.dto.Score
                data class User(val score: Score)
                """.trimIndent(),
            ) as KtFile

        val result = findClassForType(file, "Score")

        assertNotNull(result)
        assertEquals("com.example.dto.Score", result!!.fqName?.asString())
        assertTrue(result.isEnum())
    }

    fun `test resolves data class from same package when no explicit import`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val address: Address)",
            ) as KtFile

        val result = findClassForType(file, "Address")

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    fun `test resolves same-package class over other-package class when no import`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        myFixture.addFileToProject("com/other/Address.kt", "package com.other\ndata class Address(val city: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val address: Address)",
            ) as KtFile

        val result = findClassForType(file, "Address")

        assertNotNull(result)
        assertEquals("street", result!!.primaryConstructorParameters[0].name)
    }

    fun `test resolves enum class from same package when no explicit import`() {
        myFixture.addFileToProject("com/example/Score.kt", "package com.example\nenum class Score { HIGH, LOW, MEDIUM }")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val score: Score)",
            ) as KtFile

        val result = findClassForType(file, "Score")

        assertNotNull(result)
        assertEquals("com.example.Score", result!!.fqName?.asString())
        assertTrue(result.isEnum())
    }

    fun `test returns null for regular non-data class`() {
        myFixture.addFileToProject("Foo.kt", "package com.example\nclass Foo(val x: String)")
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val foo: Foo)") as KtFile

        assertNull(findClassForType(file, "Foo"))
    }

    fun `test returns null for unknown type with no import and no same-package match`() {
        val file = myFixture.addFileToProject("User.kt", "package com.example\ndata class User(val ghost: GhostType)") as KtFile

        assertNull(findClassForType(file, "GhostType"))
    }

    // -------------------------------------------------------------------------
    // Type alias
    // -------------------------------------------------------------------------

    fun `test resolves typealias for data class to its underlying class`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ntypealias MyAddress = Address\ndata class User(val address: MyAddress)",
            ) as KtFile

        val result = findClassForType(file, "MyAddress")

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    fun `test resolves typealias for enum class to its underlying class`() {
        myFixture.addFileToProject("com/example/Score.kt", "package com.example\nenum class Score { HIGH, LOW }")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ntypealias MyScore = Score\ndata class User(val score: MyScore)",
            ) as KtFile

        val result = findClassForType(file, "MyScore")

        assertNotNull(result)
        assertEquals("com.example.Score", result!!.fqName?.asString())
        assertTrue(result.isEnum())
    }

    // -------------------------------------------------------------------------
    // Collection element resolution
    // -------------------------------------------------------------------------

    fun `test returns element class for custom collection`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val addresses: List<Address>)",
            ) as KtFile

        val result =
            findClassForType(
                file = file,
                typeName = "List<Address>",
                mapped = MappedType.CollectionType(element = "Address", customElement = true),
            )

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    fun `test returns element class for collection typealias`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ntypealias AddressList = List<Address>\ndata class User(val addresses: AddressList)",
            ) as KtFile

        val result =
            findClassForType(
                file = file,
                typeName = "AddressList",
                mapped = MappedType.CollectionType(element = "Address", customElement = true),
            )

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    // -------------------------------------------------------------------------
    // Map value resolution
    // -------------------------------------------------------------------------

    fun `test returns value class for custom map value`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ndata class User(val addresses: Map<String, Address>)",
            ) as KtFile

        val result =
            findClassForType(
                file = file,
                typeName = "Map<String, Address>",
                mapped = MappedType.MapType(key = "string", value = "Address", customValue = true),
            )

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    fun `test returns value class for map value typealias`() {
        myFixture.addFileToProject("com/example/Address.kt", "package com.example\ndata class Address(val street: String)")
        val file =
            myFixture.addFileToProject(
                "com/example/User.kt",
                "package com.example\ntypealias AddressMap = Map<String, Address>\ndata class User(val addresses: AddressMap)",
            ) as KtFile

        val result =
            findClassForType(
                file = file,
                typeName = "AddressMap",
                mapped = MappedType.MapType(key = "string", value = "Address", customValue = true),
            )

        assertNotNull(result)
        assertEquals("com.example.Address", result!!.fqName?.asString())
    }

    // -------------------------------------------------------------------------
    // Known scalar
    // -------------------------------------------------------------------------

    fun `test returns null for scalar type`() {
        val file = myFixture.addFileToProject("User.kt", "data class User(val name: String)") as KtFile

        assertNull(findClassForType(file, "String", MappedType.ScalarType(type = "string", isNullable = false)))
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private fun findClassForType(
        file: KtFile,
        typeName: String,
        mapped: MappedType? = null,
    ): KtClass? =
        inSmartReadAction {
            val typeRef =
                file.children
                    .filterIsInstance<KtClass>()
                    .flatMap { it.primaryConstructorParameters }
                    .mapNotNull { it.typeReference }
                    .first { it.text.trimEnd('?') == typeName }
            KtClassResolver.findReferencedClass(typeRef, mapped)
        }
}
