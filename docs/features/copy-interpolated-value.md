# Copy Interpolated Value

**Copy Interpolated Value** is an intention action that resolves all variable references in a Kotlin string interpolation and copies the final computed string directly to your clipboard — without running the code.

---

## How It Works

Place the caret anywhere inside a property whose initializer is a string template with interpolation, then invoke **Alt+Enter** (or **Option+Return** on macOS) and select **Copy interpolated value**.

The plugin evaluates all `$variable` and `${expression}` references statically and copies the resulting string to the clipboard.

```kotlin
const val NAME = "André"
const val SURNAME = "Monteiro"
const val FULL_NAME = "$NAME $SURNAME"
// Invoke on FULL_NAME → "André Monteiro" is copied to clipboard
```

---

## Availability

The intention is only shown when:

- The caret is inside a **property** declaration (top-level or member)
- The initializer is a **string template with interpolation** (`$var` or `${expr}`)
- All referenced variables can be **statically resolved** (e.g. `const val` chains)

It will **not** appear for:

- Plain string literals with no interpolation: `"Hello, World!"`
- Strings with only escape sequences: `"line1\nline2"`
- Properties with unresolvable references: `"Hello, $UNKNOWN"`
- Local variables inside function bodies
- Properties without an initializer

---

## Limitations

!!! info "Compile-time constants only"
    Only values resolvable at compile time are supported.

- ✅ `const val` chains
- ✅ Nested interpolations (interpolating an interpolated value)
- ❌ Runtime-computed values
- ❌ Results of non-const functions
- ❌ Mutable variables (`var`)

### Example — not supported

```kotlin
fun greeting() = "Hello"
const val MSG = "${greeting()} world"
// Intention will not be available — greeting() is not a compile-time constant
```

---

## K2 Compatibility

!!! success "Fully Compatible"
    Works with both K1 (classic) and K2 Kotlin compiler modes.

- ✅ Kotlin K1 (Classic Compiler)
- ✅ Kotlin K2 (New Compiler)

---

<div style="text-align: center;" markdown="1">

**Explore more features!**

[Kotlin Inlay Hints](kotlin-inlay-hints.md){ .md-button }
[Wrap with Coroutine Builder](wrap-with-coroutine.md){ .md-button }

</div>
