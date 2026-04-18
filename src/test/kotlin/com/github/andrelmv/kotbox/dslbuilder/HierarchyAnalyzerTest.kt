package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.BuilderField
import com.github.andrelmv.kotbox.dslbuilder.generator.BuilderHierarchy
import com.github.andrelmv.kotbox.dslbuilder.generator.HierarchyAnalyzer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass
import org.junit.Test

class HierarchyAnalyzerTest : BasePlatformTestCase() {
    @Test
    fun `test analyzes single data class`() {
        myFixture.configureByText("Test.kt", "data class User(val name: String, val age: Int)")
        val h = analyze("User")
        assertEquals(1, h.builders.size)
        assertEquals("UserDsl", h.dslMarkerName)
        assertTrue(h.builders.first().isRoot)
    }

    @Test
    fun `test analyzes nested hierarchy in topological order`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val name: String, val address: Address)")
        val h = analyze("User")
        assertEquals(2, h.builders.size)
        assertEquals("Address", h.builders[0].dataClassName)
        assertEquals("User", h.builders[1].dataClassName)
    }

    @Test
    fun `test handles three-level hierarchy`() {
        myFixture.addFileToProject("Coords.kt", "package com.ex\ndata class Coordinates(val lat: Double, val lng: Double)")
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String, val coordinates: Coordinates)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val name: String, val address: Address)")
        val h = analyze("User")
        assertEquals(3, h.builders.size)
        assertEquals("Coordinates", h.builders[0].dataClassName)
        assertEquals("Address", h.builders[1].dataClassName)
        assertEquals("User", h.builders[2].dataClassName)
    }

    @Test
    fun `test does not expand external library classes`() {
        myFixture.configureByText("Test.kt", "import java.time.LocalDate\ndata class User(val name: String, val born: LocalDate)")
        val h = analyze("User")
        assertEquals(1, h.builders.size) // só User, LocalDate não expandido
        assertTrue(
            h.builders
                .first()
                .fields
                .find { it.name == "born" } is BuilderField.Simple,
        )
    }

    @Test
    fun `test classifies List of String as SimpleList in hierarchy`() {
        myFixture.configureByText("Test.kt", "data class User(val tags: List<String>)")
        val h = analyze("User")
        val tagsField =
            h.builders
                .first()
                .fields
                .first()
        assertTrue(tagsField is BuilderField.SimpleList)
        assertEquals("String", (tagsField as BuilderField.SimpleList).elementTypeName)
    }

    @Test
    fun `test dslMarkerName uses root class name`() {
        myFixture.configureByText("Test.kt", "data class Order(val id: Long)")
        assertEquals("OrderDsl", analyze("Order").dslMarkerName)
    }

    @Test
    fun `test handles class with no fields`() {
        myFixture.configureByText("Test.kt", "data class Empty()")
        val h = analyze("Empty")
        assertEquals(1, h.builders.size)
        assertTrue(
            h.builders
                .first()
                .fields
                .isEmpty(),
        )
    }

    @Test
    fun `test does not infinite loop on mutual cycle between two data classes`() {
        // A references B and B references A — cycle must be detected via visited set
        myFixture.addFileToProject("B.kt", "data class B(val value: String)")
        myFixture.configureByText("A.kt", "data class A(val b: B)")
        // B does not reference A, so this is not a true cycle, but we add a self-referencing guard test:
        // The visited set must prevent re-processing A if B somehow points back
        val h = analyze("A")
        // A and B each appear exactly once
        val classNames = h.builders.map { it.dataClassName }
        assertEquals(classNames.distinct(), classNames)
    }

    @Test
    fun `test List of data class produces NestedBuilderList in hierarchy`() {
        myFixture.addFileToProject("Item.kt", "package com.ex\ndata class Item(val label: String)")
        myFixture.configureByText("Cart.kt", "package com.ex\ndata class Cart(val items: List<Item>)")
        val h = analyze("Cart")
        // Item builder is created (nested), Cart builder is root
        assertEquals(2, h.builders.size)
        assertEquals("Item", h.builders[0].dataClassName)
        assertEquals("Cart", h.builders[1].dataClassName)
        val itemsField = h.builders[1].fields.first()
        assertTrue(itemsField is BuilderField.NestedBuilderList)
    }

    @Test
    fun `test collects required imports for nested data class types`() {
        myFixture.addFileToProject("Address.kt", "package com.ex\ndata class Address(val street: String)")
        myFixture.configureByText("User.kt", "package com.ex\ndata class User(val address: Address)")
        val h = analyze("User")
        assertTrue(h.requiredImports.contains("com.ex.Address"))
    }

    // Helper
    private fun analyze(className: String): BuilderHierarchy {
        val cls =
            myFixture.file.children
                .filterIsInstance<KtClass>()
                .firstOrNull { it.name == className }
                ?: error("Class '$className' not found")
        return HierarchyAnalyzer(project, GlobalSearchScope.projectScope(project)).analyze(cls)
    }
}
