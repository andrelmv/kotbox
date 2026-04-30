package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.generator.BuilderClassModel
import com.github.andrelmv.kotbox.dslbuilder.generator.BuilderField
import com.github.andrelmv.kotbox.dslbuilder.generator.BuilderHierarchy
import com.github.andrelmv.kotbox.dslbuilder.generator.CodeRenderer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRendererTest {
    private val renderer = CodeRenderer()

    @Test
    fun `renders DslMarker annotation`() {
        val result = renderer.render(hierarchy(emptyList(), dslMarkerName = "UserDsl"))
        assertTrue(result.contains("@DslMarker"))
        assertTrue(result.contains("annotation class UserDsl"))
    }

    @Test
    fun `renders required Simple field with error in build`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("name", "String", false, isRequired = true),
                    ),
                ),
            )
        assertTrue(result.contains("var name: String? = null"))
        assertTrue(result.contains("name ?: error("))
        assertTrue(result.contains("'name' is required"))
    }

    @Test
    fun `renders optional field without error`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("nickname", "String", true, isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("nickname = nickname,"))
        assertFalse(result.contains("nickname ?: error("))
    }

    @Test
    fun `renders NestedBuilder as private var with DSL method`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.NestedBuilder("address", "Address", "AddressBuilder", isRequired = true),
                    ),
                ),
            )
        assertTrue(result.contains("private var address: Address? = null"))
        assertTrue(result.contains("fun address(block: AddressBuilder.() -> Unit)"))
        assertTrue(result.contains("address = AddressBuilder().apply(block).build()"))
    }

    @Test
    fun `renders SimpleList as MutableList with vararg accessor and toList in build`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.SimpleList("tags", "String", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private val tags: MutableList<String> = mutableListOf()"))
        assertTrue(result.contains("fun tags(vararg items: String)"))
        assertTrue(result.contains("tags.addAll(items.toList())"))
        assertTrue(result.contains("tags = tags.toList(),"))
    }

    @Test
    fun `renders SimpleSet with vararg accessor and toSet in build`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.SimpleSet("codes", "String", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private val codes: MutableSet<String> = mutableSetOf()"))
        assertTrue(result.contains("fun codes(vararg items: String)"))
        assertTrue(result.contains("codes.addAll(items.toList())"))
        assertTrue(result.contains("codes = codes.toSet(),"))
    }

    @Test
    fun `renders SimpleMap with key-value accessor and toMap in build`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.SimpleMap("meta", "String", "Int", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private val meta: MutableMap<String, Int> = mutableMapOf()"))
        assertTrue(result.contains("fun meta(key: String, value: Int)"))
        assertTrue(result.contains("meta[key] = value"))
        assertTrue(result.contains("meta = meta.toMap(),"))
    }

    @Test
    fun `renders NestedBuilderList with add method`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.NestedBuilderList("roles", "Role", "RoleBuilder", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("fun roles(block: RoleBuilder.() -> Unit)"))
        assertTrue(result.contains("roles.add(RoleBuilder().apply(block).build())"))
    }

    @Test
    fun `renders entry function only for root`() {
        val result = renderer.render(hierarchy(emptyList(), dataClassName = "User", isRoot = true))
        assertTrue(result.contains("fun user(block: UserBuilder.() -> Unit): User"))

        val resultNonRoot = renderer.render(hierarchy(emptyList(), dataClassName = "Address", isRoot = false))
        assertFalse(resultNonRoot.contains("fun address("))
    }

    @Test
    fun `renders import statements when requiredImports is non-empty`() {
        val h =
            BuilderHierarchy(
                builders =
                    listOf(
                        BuilderClassModel(
                            "User",
                            "UserBuilder",
                            "com.example",
                            emptyList(),
                            "UserDsl",
                            true,
                        ),
                    ),
                dslMarkerName = "UserDsl",
                requiredImports = setOf("com.example.Address", "com.example.Coordinates"),
            )
        val result = renderer.render(h)
        assertTrue(result.contains("import com.example.Address"))
        assertTrue(result.contains("import com.example.Coordinates"))
        // imports must appear before @DslMarker
        assertTrue(result.indexOf("import") < result.indexOf("@DslMarker"))
    }

    @Test
    fun `skips imports for types in the same package as the generated file`() {
        val h =
            BuilderHierarchy(
                builders =
                    listOf(
                        BuilderClassModel(
                            "User",
                            "UserBuilder",
                            "com.example",
                            emptyList(),
                            "UserDsl",
                            true,
                        ),
                    ),
                dslMarkerName = "UserDsl",
                requiredImports = setOf("com.example.Address", "other.pkg.Coordinates"),
            )
        // Address is in the same package — should be omitted
        val result = renderer.render(h, ownPackage = "com.example")
        assertFalse(result.contains("import com.example.Address"))
        // Coordinates is in a different package — must be kept
        assertTrue(result.contains("import other.pkg.Coordinates"))
    }

    @Test
    fun `emits all imports when ownPackage is blank`() {
        val h =
            BuilderHierarchy(
                builders =
                    listOf(
                        BuilderClassModel(
                            "User",
                            "UserBuilder",
                            "com.example",
                            emptyList(),
                            "UserDsl",
                            true,
                        ),
                    ),
                dslMarkerName = "UserDsl",
                requiredImports = setOf("com.example.Address"),
            )
        val result = renderer.render(h, ownPackage = "")
        assertTrue(result.contains("import com.example.Address"))
    }

    @Test
    fun `renders full hierarchy in topological order`() {
        val h =
            BuilderHierarchy(
                builders =
                    listOf(
                        BuilderClassModel(
                            "Coordinates",
                            "CoordinatesBuilder",
                            "com.example",
                            listOf(BuilderField.Simple("lat", "Double", false, true)),
                            "UserDsl",
                            false,
                        ),
                        BuilderClassModel(
                            "User",
                            "UserBuilder",
                            "com.example",
                            listOf(BuilderField.NestedBuilder("coords", "Coordinates", "CoordinatesBuilder", true)),
                            "UserDsl",
                            true,
                        ),
                    ),
                dslMarkerName = "UserDsl",
                requiredImports = emptySet(),
            )
        val result = renderer.render(h)
        assertTrue(result.indexOf("CoordinatesBuilder") < result.indexOf("UserBuilder"))
        assertTrue(result.contains("fun user(block: UserBuilder.() -> Unit): User"))
    }

    @Test
    fun `wraps function type in parens before adding nullable marker`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("cenas", "(s: String, i: Int) -> String", false, isRequired = true),
                    ),
                ),
            )
        assertTrue(result.contains("var cenas: ((s: String, i: Int) -> String)? = null"))
        assertFalse(result.contains("-> String? ="))
    }

    @Test
    fun `renders builder with no fields without extra blank lines`() {
        val result = renderer.render(hierarchy(emptyList()))
        // build() method must be present
        assertTrue(result.contains("fun build(): User"))
        // no double blank line between class opening brace and build method
        assertFalse(result.contains("{\n\n\n"))
    }

    @Test
    fun `renders optional NestedBuilder without error in build`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.NestedBuilder("address", "Address", "AddressBuilder", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private var address: Address? = null"))
        // optional nested builder: no error(), just pass through as-is
        assertTrue(result.contains("address = address,"))
        assertFalse(result.contains("address ?: error("))
    }

    @Test
    fun `renders required NestedBuilderList using toList regardless of isRequired`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.NestedBuilderList("roles", "Role", "RoleBuilder", isRequired = true),
                    ),
                ),
            )
        // collection fields always use toList() in build — isRequired has no effect
        assertTrue(result.contains("roles = roles.toList(),"))
        assertFalse(result.contains("roles ?: error("))
    }

    @Test
    fun `renders required SimpleList using toList regardless of isRequired`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.SimpleList("tags", "String", isRequired = true),
                    ),
                ),
            )
        assertTrue(result.contains("tags = tags.toList(),"))
        assertFalse(result.contains("tags ?: error("))
    }

    @Test
    fun `renders all field types combined in one builder`() {
        val fields =
            listOf(
                BuilderField.Simple("name", "String", false, isRequired = true),
                BuilderField.NestedBuilder("address", "Address", "AddressBuilder", isRequired = true),
                BuilderField.SimpleList("tags", "String", isRequired = false),
                BuilderField.NestedBuilderList("roles", "Role", "RoleBuilder", isRequired = false),
                BuilderField.SimpleSet("codes", "Int", isRequired = false),
                BuilderField.SimpleMap("meta", "String", "Any", isRequired = false),
            )
        val result = renderer.render(hierarchy(fields))
        assertTrue(result.contains("var name: String? = null"))
        assertTrue(result.contains("private var address: Address? = null"))
        assertTrue(result.contains("private val tags: MutableList<String>"))
        assertTrue(result.contains("private val roles: MutableList<Role>"))
        assertTrue(result.contains("private val codes: MutableSet<Int>"))
        assertTrue(result.contains("private val meta: MutableMap<String, Any>"))
        assertTrue(result.contains("name ?: error("))
        assertTrue(result.contains("address ?: error("))
        assertTrue(result.contains("tags = tags.toList(),"))
        assertTrue(result.contains("roles = roles.toList(),"))
        assertTrue(result.contains("codes = codes.toSet(),"))
        assertTrue(result.contains("meta = meta.toMap(),"))
    }

    @Test
    fun `entry function lowercases only first char leaving rest intact`() {
        // "URL" → "uRL", not "url"
        val result = renderer.render(hierarchy(emptyList(), dataClassName = "URL", isRoot = true))
        assertTrue(result.contains("fun uRL(block: URLBuilder.() -> Unit): URL"))
    }

    @Test
    fun `renders function type with Unit return wrapped in parens for nullable`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("callback", "() -> Unit", false, isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("var callback: (() -> Unit)? = null"))
        assertFalse(result.contains("-> Unit? ="))
    }

    @Test
    fun `renders SimpleList with nested generic element type`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.SimpleList("matrix", "Map<String, Int>", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private val matrix: MutableList<Map<String, Int>> = mutableListOf()"))
        assertTrue(result.contains("fun matrix(vararg items: Map<String, Int>)"))
        assertTrue(result.contains("matrix = matrix.toList(),"))
    }

    @Test
    fun `multiple builders renders entry function only for last root`() {
        val h =
            BuilderHierarchy(
                builders =
                    listOf(
                        BuilderClassModel(
                            "Leaf",
                            "LeafBuilder",
                            "com.example",
                            emptyList(),
                            "RootDsl",
                            false,
                        ),
                        BuilderClassModel(
                            "Root",
                            "RootBuilder",
                            "com.example",
                            listOf(BuilderField.NestedBuilder("leaf", "Leaf", "LeafBuilder", true)),
                            "RootDsl",
                            true,
                        ),
                    ),
                dslMarkerName = "RootDsl",
                requiredImports = emptySet(),
            )
        val result = renderer.render(h)
        assertTrue(result.contains("fun root(block: RootBuilder.() -> Unit): Root"))
        assertFalse(result.contains("fun leaf(block: LeafBuilder.() -> Unit): Leaf"))
    }

    @Test
    fun `escapes Kotlin hard keyword as Simple field name with backticks`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("class", "String", false, isRequired = true),
                    ),
                ),
            )
        assertTrue(result.contains("var `class`: String? = null"))
        assertTrue(result.contains("`class` = `class` ?: error("))
        assertTrue(result.contains("'class' is required"))
    }

    @Test
    fun `escapes Kotlin hard keyword as NestedBuilder field name with backticks`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.NestedBuilder("object", "Config", "ConfigBuilder", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private var `object`: Config? = null"))
        assertTrue(result.contains("fun `object`(block: ConfigBuilder.() -> Unit)"))
        assertTrue(result.contains("`object` = ConfigBuilder().apply(block).build()"))
        assertTrue(result.contains("`object` = `object`,"))
    }

    @Test
    fun `escapes Kotlin hard keyword as SimpleList field name with backticks`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.SimpleList("in", "String", isRequired = false),
                    ),
                ),
            )
        assertTrue(result.contains("private val `in`: MutableList<String> = mutableListOf()"))
        assertTrue(result.contains("fun `in`(vararg items: String)"))
        assertTrue(result.contains("`in`.addAll(items.toList())"))
        assertTrue(result.contains("`in` = `in`.toList(),"))
    }

    @Test
    fun `renders Simple field with generic type preserving type arguments`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("type", "Class<*>", false, isRequired = true),
                    ),
                ),
            )
        assertTrue(result.contains("var type: Class<*>? = null"))
        assertFalse(result.contains("var type: Class? = null"))
    }

    @Test
    fun `does not escape non-keyword field names`() {
        val result =
            renderer.render(
                hierarchy(
                    listOf(
                        BuilderField.Simple("value", "Int", false, isRequired = false),
                    ),
                ),
            )
        // "value" is not a hard keyword — must not be wrapped in backticks
        assertTrue(result.contains("var value: Int? = null"))
        assertFalse(result.contains("`value`"))
    }

    // Helper
    private fun hierarchy(
        fields: List<BuilderField>,
        dataClassName: String = "User",
        dslMarkerName: String = "UserDsl",
        isRoot: Boolean = true,
    ) = BuilderHierarchy(
        builders =
            listOf(
                BuilderClassModel(
                    dataClassName,
                    "${dataClassName}Builder",
                    "com.example",
                    fields,
                    dslMarkerName,
                    isRoot,
                ),
            ),
        dslMarkerName = dslMarkerName,
        requiredImports = emptySet(),
    )
}
