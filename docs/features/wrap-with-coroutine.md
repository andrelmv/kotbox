# Wrap with Coroutine Builder

**Wrap with Coroutine Builder** lets you instantly wrap any selected block of Kotlin code inside a coroutine builder — directly from the editor, with the required imports added automatically.

---

## How to Use

### Via Alt+Enter (Intention Action)

1. **Select** one or more lines of code in a Kotlin file
2. Press **Alt+Enter** (or **Option+Space** on macOS)
3. Choose **"Wrap with coroutine builder..."** from the list
4. Pick the builder you want from the popup

### Via Keyboard Shortcut

Select the code and press the shortcut for the builder you want directly:

| Builder                           | Shortcut           |
|-----------------------------------|--------------------|
| `async { }`                       | `Ctrl+Alt+Cmd+Y` |
| `coroutineScope { }`              | `Ctrl+Alt+Cmd+O` |
| `launch { }`                      | `Ctrl+Alt+Cmd+J` |
| `withContext(Dispatchers.IO) { }` | `Ctrl+Alt+Cmd+W` |
| `supervisorScope { }`             | `Ctrl+Alt+Cmd+S` |
| `runBlocking { }`                 | `Ctrl+Alt+Cmd+B` |
| `withTimeout(timeMillis) { }`     | `Ctrl+Alt+Cmd+T` |

### Via Right-Click Menu

Right-click anywhere in the editor and go to **Wrap with coroutine** to find all available builders.

---

## Available Builders

### `async { }`

Starts a coroutine that computes a result asynchronously and returns a `Deferred<T>`. Use it when you need a value back from a concurrent operation.

```kotlin
// Before
val user = repository.fetchUser(id)
render(user)

// After
val deferred = async {
    val user = repository.fetchUser(id)
    render(user)
}
```

---

### `coroutineScope { }`

Creates a new coroutine scope and suspends until all child coroutines complete. Fails fast — if any child fails, the entire scope is cancelled.

```kotlin
// Before
val users = fetchUsers()
val orders = fetchOrders()

// After
coroutineScope {
    val users = fetchUsers()
    val orders = fetchOrders()
}
```

---

### `launch { }`

Launches a fire-and-forget coroutine that runs concurrently. Returns a `Job` but does not produce a result value.

```kotlin
// Before
sendAnalyticsEvent("screen_view")

// After
launch {
    sendAnalyticsEvent("screen_view")
}
```

---

### `withContext(Dispatchers.IO) { }`

Switches the coroutine to a different dispatcher for the duration of the block, then resumes on the original dispatcher. Ideal for offloading I/O operations.

```kotlin
// Before
val data = readFromDisk()

// After
val data = withContext(Dispatchers.IO) {
    readFromDisk()
}
```

!!! tip "Dispatcher"
    The default dispatcher is `Dispatchers.IO`. You can change it to `Dispatchers.Default` for CPU-intensive work or `Dispatchers.Main` to switch back to the main thread.

---

### `supervisorScope { }`

Like `coroutineScope`, but failures in child coroutines do not cancel siblings or the parent scope. Use it when children should be independent.

```kotlin
// Before
launch { loadUserProfile() }
launch { loadUserPosts() }

// After
supervisorScope {
    launch { loadUserProfile() }
    launch { loadUserPosts() }
}
```

---

### `runBlocking { }`

Blocks the current thread until all coroutines inside complete. Intended for **main functions** and **tests** — avoid it in production coroutine code.

```kotlin
// Before
val result = suspendingOperation()
println(result)

// After
runBlocking {
    val result = suspendingOperation()
    println(result)
}
```

!!! warning "Avoid in production coroutines"
    `runBlocking` blocks the calling thread. Do not use it inside a coroutine or on the main thread of a UI application.

---

### `withTimeout(timeMillis) { }`

Runs the block with a time limit. Throws `TimeoutCancellationException` if the block does not complete in time. Replace `timeMillis` with the actual duration.

```kotlin
// Before
val response = networkCall()

// After
val response = withTimeout(timeMillis) {
    networkCall()
}
```

!!! tip "Return null instead of throwing"
    Use `withTimeoutOrNull(timeMillis) { }` if you prefer a `null` result on timeout instead of an exception.

---

## Automatic Imports

The plugin automatically adds the required imports — you never have to type them manually:

| Builder           | Imports added                                                      |
|-------------------|--------------------------------------------------------------------|
| `async`           | `kotlinx.coroutines.async`                                         |
| `coroutineScope`  | `kotlinx.coroutines.coroutineScope`                                |
| `launch`          | `kotlinx.coroutines.launch`                                        |
| `withContext`     | `kotlinx.coroutines.withContext`, `kotlinx.coroutines.Dispatchers` |
| `supervisorScope` | `kotlinx.coroutines.supervisorScope`                               |
| `runBlocking`     | `kotlinx.coroutines.runBlocking`                                   |
| `withTimeout`     | `kotlinx.coroutines.withTimeout`                                   |

If an import is already present it will not be duplicated.

---

## Indentation

The engine detects the indentation of the selected block and preserves it in the generated code. Wrapping deeply nested code keeps everything properly aligned.

```kotlin
class MyViewModel {
    fun load() {
        // Select this line and wrap with launch:
        fetchData()

        // Result:
        launch {
            fetchData()
        }
    }
}
```

---

## Customising Shortcuts

All shortcuts can be remapped via **Settings → Keymap → Kotlin Toolbox**:

1. Go to **Settings / Preferences**
2. Navigate to **Keymap**
3. Search for **"Wrap with"**
4. Double-click the action and assign your preferred shortcut

---

## Compatibility

!!! success "Fully Compatible"
    Works with both the classic Kotlin compiler (K1) and the new K2 compiler.

- ✅ Kotlin K1 (Classic Compiler)
- ✅ Kotlin K2 (New Compiler)
- ✅ IntelliJ IDEA 2025.3.2 or later
- ✅ Kotlin plugin enabled

---

<div style="text-align: center;" markdown="1">

**Explore more features!**

[JWT Encoder/Decoder](jwt-encoder-decoder.md){ .md-button }
[Kotlin Inlay Hints](kotlin-inlay-hints.md){ .md-button }

</div>
