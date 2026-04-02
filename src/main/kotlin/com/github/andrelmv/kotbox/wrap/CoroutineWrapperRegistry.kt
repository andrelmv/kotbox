package com.github.andrelmv.kotbox.wrap

/**
 * Central registry of all available coroutine wrappers.
 *
 * To add a new wrapper:
 *   1. Add a [WrapperDescriptor] to this list.
 *   2. Register the corresponding AnAction in plugin.xml (see the Actions section).
 *
 * The IntentionAction iterates over this list automatically — no changes
 * to the intention are needed when new wrappers are added.
 */
object CoroutineWrapperRegistry {
    val all: List<WrapperDescriptor> =
        listOf(
            WrapperDescriptor(
                id = "async",
                displayName = "Wrap with async { }",
                wrapTemplate = "async {\n%s\n}",
                requiredImports = listOf("kotlinx.coroutines.async"),
                actionId = "KotlinToolbox.WrapAsync",
            ),
            WrapperDescriptor(
                id = "coroutineScope",
                displayName = "Wrap with coroutineScope { }",
                wrapTemplate = "coroutineScope {\n%s\n}",
                requiredImports = listOf("kotlinx.coroutines.coroutineScope"),
                actionId = "KotlinToolbox.WrapCoroutineScope",
            ),
            WrapperDescriptor(
                id = "launch",
                displayName = "Wrap with launch { }",
                wrapTemplate = "launch {\n%s\n}",
                requiredImports = listOf("kotlinx.coroutines.launch"),
                actionId = "KotlinToolbox.WrapLaunch",
            ),
            WrapperDescriptor(
                id = "withContext",
                displayName = "Wrap with withContext(Dispatchers.IO) { }",
                wrapTemplate = "withContext(Dispatchers.IO) {\n%s\n}",
                requiredImports =
                    listOf(
                        "kotlinx.coroutines.withContext",
                        "kotlinx.coroutines.Dispatchers",
                    ),
                actionId = "KotlinToolbox.WrapWithContext",
            ),
            WrapperDescriptor(
                id = "supervisorScope",
                displayName = "Wrap with supervisorScope { }",
                wrapTemplate = "supervisorScope {\n%s\n}",
                requiredImports = listOf("kotlinx.coroutines.supervisorScope"),
                actionId = "KotlinToolbox.WrapSupervisorScope",
            ),
            WrapperDescriptor(
                id = "runBlocking",
                displayName = "Wrap with runBlocking { }",
                wrapTemplate = "runBlocking {\n%s\n}",
                requiredImports = listOf("kotlinx.coroutines.runBlocking"),
                actionId = "KotlinToolbox.WrapRunBlocking",
            ),
            WrapperDescriptor(
                id = "withTimeout",
                displayName = "Wrap with withTimeout(timeMillis) { }",
                wrapTemplate = "withTimeout(timeMillis) {\n%s\n}",
                requiredImports = listOf("kotlinx.coroutines.withTimeout"),
                actionId = "KotlinToolbox.WrapWithTimeout",
            ),
        )

    fun findById(id: String): WrapperDescriptor? = all.firstOrNull { it.id == id }
}
