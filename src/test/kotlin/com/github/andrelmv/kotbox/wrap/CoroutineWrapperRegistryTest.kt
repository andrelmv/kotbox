package com.github.andrelmv.kotbox.wrap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineWrapperRegistryTest {
    // ──────────────────────────────────────────────────────────────
    // Structural integrity
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `registry contains exactly 7 wrappers`() {
        assertEquals(7, CoroutineWrapperRegistry.all.size)
    }

    @Test
    fun `all wrappers have unique ids`() {
        val ids = CoroutineWrapperRegistry.all.map { it.id }
        assertEquals("Duplicate IDs found", ids.distinct().size, ids.size)
    }

    @Test
    fun `all wrappers have unique actionIds`() {
        val actionIds = CoroutineWrapperRegistry.all.map { it.actionId }
        assertEquals("Duplicate actionIds found", actionIds.distinct().size, actionIds.size)
    }

    @Test
    fun `all wrappers have non-blank displayName`() {
        CoroutineWrapperRegistry.all.forEach { descriptor ->
            assertTrue(
                "Wrapper ${descriptor.id} has blank displayName",
                descriptor.displayName.isNotBlank(),
            )
        }
    }

    @Test
    fun `all wrappers template contains format placeholder`() {
        CoroutineWrapperRegistry.all.forEach { descriptor ->
            assertTrue(
                "Wrapper ${descriptor.id} missing %s in template",
                descriptor.wrapTemplate.contains("%s"),
            )
        }
    }

    @Test
    fun `all wrappers have non-blank actionId`() {
        CoroutineWrapperRegistry.all.forEach { descriptor ->
            assertTrue(
                "Wrapper ${descriptor.id} has blank actionId",
                descriptor.actionId.isNotBlank(),
            )
        }
    }

    @Test
    fun `all wrappers have at least one required import`() {
        CoroutineWrapperRegistry.all.forEach { descriptor ->
            assertTrue(
                "Wrapper ${descriptor.id} has no requiredImports",
                descriptor.requiredImports.isNotEmpty(),
            )
        }
    }

    @Test
    fun `all required imports are valid kotlinx coroutines fqnames`() {
        CoroutineWrapperRegistry.all.forEach { descriptor ->
            descriptor.requiredImports.forEach { import ->
                assertTrue(
                    "Wrapper ${descriptor.id} has invalid import: $import",
                    import.startsWith("kotlinx.coroutines."),
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // findById
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `findById returns null for unknown id`() {
        assertNull(CoroutineWrapperRegistry.findById("doesNotExist"))
    }

    @Test
    fun `findById is case-sensitive`() {
        assertNull(CoroutineWrapperRegistry.findById("Async"))
        assertNull(CoroutineWrapperRegistry.findById("ASYNC"))
        assertNull(CoroutineWrapperRegistry.findById("Launch"))
    }

    // ──────────────────────────────────────────────────────────────
    // async
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `async descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("async")!!
        assertEquals("async", d.id)
        assertEquals("Wrap with async { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.async"))
        assertEquals("KotlinToolbox.WrapAsync", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("async {"))
    }

    // ──────────────────────────────────────────────────────────────
    // coroutineScope
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `coroutineScope descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("coroutineScope")!!
        assertEquals("coroutineScope", d.id)
        assertEquals("Wrap with coroutineScope { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.coroutineScope"))
        assertEquals("KotlinToolbox.WrapCoroutineScope", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("coroutineScope {"))
    }

    // ──────────────────────────────────────────────────────────────
    // launch
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `launch descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("launch")!!
        assertEquals("launch", d.id)
        assertEquals("Wrap with launch { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.launch"))
        assertEquals("KotlinToolbox.WrapLaunch", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("launch {"))
    }

    // ──────────────────────────────────────────────────────────────
    // withContext
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `withContext descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("withContext")!!
        assertEquals("withContext", d.id)
        assertEquals("Wrap with withContext(Dispatchers.IO) { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.withContext"))
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.Dispatchers"))
        assertEquals(2, d.requiredImports.size)
        assertEquals("KotlinToolbox.WrapWithContext", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("withContext(Dispatchers.IO) {"))
    }

    // ──────────────────────────────────────────────────────────────
    // supervisorScope
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `supervisorScope descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("supervisorScope")!!
        assertEquals("supervisorScope", d.id)
        assertEquals("Wrap with supervisorScope { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.supervisorScope"))
        assertEquals("KotlinToolbox.WrapSupervisorScope", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("supervisorScope {"))
    }

    // ──────────────────────────────────────────────────────────────
    // runBlocking
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `runBlocking descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("runBlocking")!!
        assertEquals("runBlocking", d.id)
        assertEquals("Wrap with runBlocking { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.runBlocking"))
        assertEquals("KotlinToolbox.WrapRunBlocking", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("runBlocking {"))
    }

    // ──────────────────────────────────────────────────────────────
    // withTimeout
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `withTimeout descriptor is correct`() {
        val d = CoroutineWrapperRegistry.findById("withTimeout")!!
        assertEquals("withTimeout", d.id)
        assertEquals("Wrap with withTimeout(timeMillis) { }", d.displayName)
        assertTrue(d.requiredImports.contains("kotlinx.coroutines.withTimeout"))
        assertEquals("KotlinToolbox.WrapWithTimeout", d.actionId)
        assertTrue(d.wrapTemplate.startsWith("withTimeout(timeMillis) {"))
    }

    // ──────────────────────────────────────────────────────────────
    // Template format contract
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `template format produces valid code for single-line body`() {
        val body = "    doSomething()"
        CoroutineWrapperRegistry.all.forEach { descriptor ->
            val result = descriptor.wrapTemplate.format(body)
            assertTrue(
                "Wrapper ${descriptor.id} template result does not contain body",
                result.contains(body),
            )
            assertTrue(
                "Wrapper ${descriptor.id} template result does not close brace",
                result.contains("}"),
            )
        }
    }

    @Test
    fun `each wrapper displayName mentions its builder name`() {
        val expectedKeywords =
            mapOf(
                "async" to "async",
                "coroutineScope" to "coroutineScope",
                "launch" to "launch",
                "withContext" to "withContext",
                "supervisorScope" to "supervisorScope",
                "runBlocking" to "runBlocking",
                "withTimeout" to "withTimeout",
            )
        expectedKeywords.forEach { (id, keyword) ->
            val d = CoroutineWrapperRegistry.findById(id)!!
            assertTrue(
                "Wrapper $id displayName does not mention '$keyword'",
                d.displayName.contains(keyword),
            )
        }
    }
}
