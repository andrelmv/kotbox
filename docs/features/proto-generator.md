# Proto Message Generator

**Proto Message Generator** converts Kotlin data classes into [Proto3](https://protobuf.dev/programming-guides/proto3/) message definitions in one action — with full support for nested types, enums, collections, maps and optionals.

---

## How to Use

### Via Alt+Enter (Intention Action)

1. Place the caret **inside** a `data class` declaration in a Kotlin file
2. Press **Alt+Enter** (or **Option+Space** on macOS)
3. Choose **"Generate Proto"** from the list
4. Select where to place the generated code

### Via Generate Menu

1. Place the caret **inside** a `data class` declaration in a Kotlin file
2. Press **Alt+Insert** (or **Cmd+N** on macOS) to open the **Generate** menu
3. Choose **"Generate Proto..."**

---

## Output Options

After triggering the action, a dialog lets you choose what to do with the generated output:

| Option             | Description                                                                  |
|--------------------|------------------------------------------------------------------------------|
| **New file**       | Writes a `.proto` file in the same directory as the source file              |
| **Preview & copy** | Opens a syntax-highlighted preview dialog with a one-click copy to clipboard |

The **Preview & copy** option is useful when your `.proto` files live in a separate repository or service — generate, review, copy, paste.

---

## What Gets Generated

Given a hierarchy of data classes, the plugin generates a self-contained `.proto` file with:

- `syntax = "proto3"` header
- `option java_package` and `option java_multiple_files` directives from the source file's package
- All referenced message types emitted as sibling `message` blocks
- All referenced enum types emitted before the messages that reference them
- Field names converted to `snake_case` per the proto style guide
- Field numbers assigned sequentially starting from 1

### Example

**Input:**

```kotlin
enum class Score { HIGH, LOW, MEDIUM }

data class Address(val street: String, val city: String)

data class User(
    val name: String,
    val age: Int,
    val address: Address,
    val tags: List<String>,
    val scores: Map<String, Int>,
    val nickname: String?,
    val score: Score,
)
```

**Generated output:**

```protobuf
syntax = "proto3";

option java_package = "com.example";
option java_multiple_files = true;

enum Score {
  HIGH = 0;
  LOW = 1;
  MEDIUM = 2;
}

message Address {
  string street = 1;
  string city = 2;
}

message User {
  string name = 1;
  int32 age = 2;
  Address address = 3;
  repeated string tags = 4;
  map<string, int32> scores = 5;
  optional string nickname = 6;
  Score score = 7;
}
```

---

## Type Mapping

### Scalar Types

| Kotlin      | Proto3                |
|-------------|-----------------------|
| `String`    | `string`              |
| `Int`       | `int32`               |
| `Long`      | `int64`               |
| `Short`     | `int32`               |
| `Byte`      | `int32`               |
| `Float`     | `float`               |
| `Double`    | `double`              |
| `Boolean`   | `bool`                |
| `ByteArray` | `bytes`               |
| `Any`       | `google.protobuf.Any` |

### Structural Types

| Kotlin                     | Proto3                                 |
|----------------------------|----------------------------------------|
| `T?`                       | `optional T`                           |
| `List<T>` / `Set<T>`       | `repeated T`                           |
| `Map<K, V>`                | `map<K, V>`                            |
| `data class` (same module) | Nested `message` block                 |
| `enum class` (same module) | `enum` block                           |
| Unknown type               | Field emitted with a `// TODO` comment |

### Map Key Restrictions

Proto3 only allows scalar types as map keys — `float`, `double`, and message types are not valid. If an invalid key type is encountered the field is treated as unresolved and a `// TODO` comment is emitted.

---

## Nullable Fields

Nullable Kotlin types map to `optional` in Proto3:

```kotlin
val nickname: String?   →   optional string nickname = 6;
val address: Address?   →   optional Address address = 3;
```

> `List<T>?` and `Map<K, V>?` — the nullable wrapper is ignored and the field is emitted as `repeated` or `map` respectively, since Proto3 has no concept of a nullable repeated field nor map.

---

## Enum Support

Kotlin `enum class` types are detected automatically and emitted as Proto3 `enum` blocks. Enum entries are converted to `UPPER_SNAKE_CASE` and numbered from 0 based on declaration order — satisfying Proto3's requirement that the first value is always 0.

```kotlin
enum class Status { ACTIVE, INACTIVE }
```

```proto
enum Status {
  ACTIVE = 0;
  INACTIVE = 1;
}
```

---

## Unresolved Types

Types that cannot be mapped — external library types, generics, or types from other modules — are emitted with a `// TODO` comment so you know exactly what needs manual attention:

```proto
// TODO: unresolved type 'LocalDate' — update manually
LocalDate born = 3;
```

---

## Syntax Highlighting in Preview

The **Preview & copy** dialog renders the generated proto with syntax highlighting:

- If the [Protocol Buffers](https://plugins.jetbrains.com/plugin/14004-protocol-buffers) plugin is installed — full proto syntax highlighting via the IDE's language support
- Otherwise, a plain text viewer with no syntax highlighting

---

## Compatibility

!!! success "Fully Compatible"
Built on the Kotlin K2 Analysis API.

- ✅ IntelliJ IDEA 2026.1 or later
- ✅ Kotlin plugin enabled
- ✅ Protocol Buffers plugin (optional — enhances preview highlighting)

---

<div style="text-align: center;" markdown="1">

**Explore more features!**

[DSL Builder Generator](dsl-builder-generator.md){ .md-button }
[Wrap with Coroutine Builder](wrap-with-coroutine.md){ .md-button }

</div>
