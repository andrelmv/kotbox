# DSL Builder Generator

**DSL Builder Generator** generates type-safe DSL builders for your Kotlin data classes — complete with `@DslMarker`, nested hierarchy support, and collection accessors — directly from the editor.

---

## How to Use

### Via Alt+Enter (Intention Action)

1. Place the caret **inside** a `data class` declaration in a Kotlin file
2. Press **Alt+Enter** (or **Option+Space** on macOS)
3. Choose **"Generate DSL builder"** from the list
4. Select where to place the generated code

### Via Generate Menu

1. Place the caret inside a `data class`
2. Press **Alt+Insert** (or **Cmd+N** on macOS) to open the **Generate** menu
3. Choose **"DSL Builder..."**

### Via Keyboard Shortcut

Place the caret inside a `data class` and press **Alt+Shift+D**.

---

## Placement Options

After triggering the action a dialog lets you choose where the generated code is written:

| Option        | Description                                                                 |
|---------------|-----------------------------------------------------------------------------|
| **Same file** | Appends the builders at the bottom of the current file                     |
| **New file**  | Creates a new `.kt` file in the same package with a customisable file name |

---

## What Gets Generated

Given a hierarchy of data classes, the plugin generates:

- A `@DslMarker` annotation scoped to the root class name
- A `Builder` class for each data class in the hierarchy, in topological order
- Typed DSL methods for nested data classes and collections
- A top-level entry function for the root class

### Example

**Input:**

```kotlin
data class Coordinates(val lat: Double, val lng: Double)
data class Address(val street: String, val city: String, val coordinates: Coordinates)
data class User(val id: Long, val name: String, val address: Address, val roles: List<String>)
```

**Generated output:**

```kotlin
@DslMarker
annotation class UserDsl

@UserDsl
class CoordinatesBuilder {
    var lat: Double? = null
    var lng: Double? = null

    fun build(): Coordinates = Coordinates(
        lat = lat ?: error("CoordinatesBuilder: 'lat' is required"),
        lng = lng ?: error("CoordinatesBuilder: 'lng' is required"),
    )
}

@UserDsl
class AddressBuilder {
    var street: String? = null
    var city: String? = null
    private var coordinates: Coordinates? = null

    fun coordinates(block: CoordinatesBuilder.() -> Unit) {
        coordinates = CoordinatesBuilder().apply(block).build()
    }

    fun build(): Address = Address(
        street = street ?: error("AddressBuilder: 'street' is required"),
        city = city ?: error("AddressBuilder: 'city' is required"),
        coordinates = coordinates ?: error("AddressBuilder: 'coordinates' is required"),
    )
}

@UserDsl
class UserBuilder {
    var id: Long? = null
    var name: String? = null
    private var address: Address? = null
    private val roles: MutableList<String> = mutableListOf()

    fun address(block: AddressBuilder.() -> Unit) {
        address = AddressBuilder().apply(block).build()
    }

    fun roles(vararg items: String) {
        roles.addAll(items.toList())
    }

    fun build(): User = User(
        id = id ?: error("UserBuilder: 'id' is required"),
        name = name ?: error("UserBuilder: 'name' is required"),
        address = address ?: error("UserBuilder: 'address' is required"),
        roles = roles.toList(),
    )
}

fun user(block: UserBuilder.() -> Unit): User =
    UserBuilder().apply(block).build()
```

**Usage:**

```kotlin
val user = user {
    id = 1L
    name = "André"
    address {
        street = "Rua das Flores"
        city = "Lisboa"
        coordinates {
            lat = 38.7223
            lng = -9.1393
        }
    }
    roles("admin", "editor")
}
```

---

## Field Type Handling

The plugin analyses every constructor parameter and generates the appropriate builder field:

| Parameter type                    | Generated builder field                                     |
|-----------------------------------|-------------------------------------------------------------|
| Primitive / `String` / enum       | `var field: Type? = null`                                   |
| `data class` (same module)        | `private var field: Type? = null` + DSL method              |
| `data class` (external library)   | Treated as a simple value — `var field: Type? = null`       |
| `T?` nullable                     | `var field: T? = null` — no `error()` in `build()`          |
| Field with default value          | `var field: T? = null` — no `error()` in `build()`          |
| `List<T>` / `MutableList<T>`      | `MutableList<T>` + `fun field(vararg items: T)`             |
| `List<data class>` (same module)  | `MutableList<T>` + `fun field(block: TBuilder.() -> Unit)`  |
| `Set<T>` / `MutableSet<T>`        | `MutableSet<T>` + `fun field(vararg items: T)`              |
| `Map<K, V>` / `MutableMap<K, V>`  | `MutableMap<K, V>` + `fun field(key: K, value: V)`          |
| `(A, B) -> R` function type       | `var field: ((A, B) -> R)? = null`                          |

---

## Required vs Optional Fields

A field is considered **required** if its type is non-nullable **and** it has no default value. Required fields throw a descriptive error at `build()` time if not set:

```kotlin
name = name ?: error("UserBuilder: 'name' is required")
```

Fields that are nullable or have a default value are silently passed through:

```kotlin
nickname = nickname,   // nullable — no error
active = active,       // has default — no error
```

---

## Nested Hierarchy

The plugin analyses the entire hierarchy of data classes recursively. Only data classes from the **same module** are expanded into nested builders — external library types (e.g. `java.time.LocalDate`) are treated as simple values.

!!! tip "Cycle detection"
    Circular references between data classes are detected automatically. Each class appears exactly once in the generated output.

---

## Idempotency

If a builder class with the same name already exists in the project, the plugin asks for confirmation before generating:

> `'UserBuilder' already exists. Overwrite?`

---

## `@DslMarker` Scope

The generated `@DslMarker` annotation prevents implicit receiver leaking between nested builder scopes, which is a common source of bugs in hand-written DSLs:

```kotlin
user {
    address {
        // 'id = ...' here would be a compile error — UserBuilder is not in scope
        street = "Rua das Flores"
    }
}
```

---

## Compatibility

!!! success "Fully Compatible"
    Built exclusively on the K2 Analysis API — no `BindingContext` usage.

- ✅ Kotlin K2 (New Compiler)
- ✅ IntelliJ IDEA 2026.1 or later
- ✅ Kotlin plugin enabled

---

<div style="text-align: center;" markdown="1">

**Explore more features!**

[Wrap with Coroutine Builder](wrap-with-coroutine.md){ .md-button }
[Kotlin Inlay Hints](kotlin-inlay-hints.md){ .md-button }

</div>
