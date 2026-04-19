package com.github.andrelmv.kotbox.wrap

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class WrapSelectionEngineTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData/wrap"

    fun `test isAvailable returns false when no selection`() {
        myFixture.configureByText("Test.kt", "fun test() {}")
        assertFalse(WrapSelectionEngine.isAvailable(myFixture.editor, myFixture.file))
    }

    fun `test isAvailable returns false for non-Kotlin file`() {
        myFixture.configureByText("Test.java", "class Test {}")
        myFixture.editor.selectionModel.setSelection(0, 5)
        assertFalse(WrapSelectionEngine.isAvailable(myFixture.editor, myFixture.file))
    }

    fun `test isAvailable returns true with selection in Kotlin file`() {
        myFixture.configureByText("Test.kt", "fun test() {}")
        myFixture.editor.selectionModel.setSelection(0, 3)
        assertTrue(WrapSelectionEngine.isAvailable(myFixture.editor, myFixture.file))
    }

    fun `test isAvailable returns false for xml file with selection`() {
        myFixture.configureByText("config.xml", "<root/>")
        myFixture.editor.selectionModel.setSelection(0, 5)
        assertFalse(WrapSelectionEngine.isAvailable(myFixture.editor, myFixture.file))
    }

    fun `test wrap with async adds async braces and import`() {
        myFixture.configureByText(
            "Test.kt",
            """
            import kotlinx.coroutines.CoroutineScope

            fun test(scope: CoroutineScope) {
                <selection>val result = doSomething()
                println(result)</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("async")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async

            fun test(scope: CoroutineScope) {
                async {
                    val result = doSomething()
                    println(result)
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with coroutineScope`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>val x = 1</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("coroutineScope")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.coroutineScope

            fun test() {
                coroutineScope {
                    val x = 1
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with launch adds braces and import`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>doWork()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("launch")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.launch

            fun test() {
                launch {
                    doWork()
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with launch handles multiline selection`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>val a = fetchA()
                val b = fetchB()
                process(a, b)</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("launch")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.launch

            fun test() {
                launch {
                    val a = fetchA()
                    val b = fetchB()
                    process(a, b)
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with withContext adds braces and import`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>readFile()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("withContext")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.withContext
            import kotlinx.coroutines.Dispatchers

            fun test() {
                withContext(Dispatchers.IO) {
                    readFile()
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with withContext handles multiline selection`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>val data = readFile()
                val parsed = parse(data)</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("withContext")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.withContext
            import kotlinx.coroutines.Dispatchers

            fun test() {
                withContext(Dispatchers.IO) {
                    val data = readFile()
                    val parsed = parse(data)
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with supervisorScope adds braces and import`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>launch { child1() }
                launch { child2() }</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("supervisorScope")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.supervisorScope

            fun test() {
                supervisorScope {
                    launch { child1() }
                    launch { child2() }
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with runBlocking adds braces and import`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun main() {
                <selection>val result = suspendFun()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("runBlocking")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.runBlocking

            fun main() {
                runBlocking {
                    val result = suspendFun()
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with runBlocking handles multiline selection`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun main() {
                <selection>val a = suspendA()
                val b = suspendB()
                println(a + b)</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("runBlocking")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.runBlocking

            fun main() {
                runBlocking {
                    val a = suspendA()
                    val b = suspendB()
                    println(a + b)
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with withTimeout adds braces and import`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>networkCall()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("withTimeout")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.withTimeout

            fun test() {
                withTimeout(timeMillis) {
                    networkCall()
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap with withTimeout handles multiline selection`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>val response = fetch()
                validate(response)</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("withTimeout")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.withTimeout

            fun test() {
                withTimeout(timeMillis) {
                    val response = fetch()
                    validate(response)
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap does not duplicate launch import when already present`() {
        myFixture.configureByText(
            "Test.kt",
            """
            import kotlinx.coroutines.launch

            fun test() {
                <selection>doWork()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("launch")!!,
        )

        val importCount =
            myFixture.file.text
                .lines()
                .count { it.trim() == "import kotlinx.coroutines.launch" }
        assertEquals("Import should appear exactly once", 1, importCount)
    }

    fun `test wrap does not duplicate async import when already present`() {
        myFixture.configureByText(
            "Test.kt",
            """
            import kotlinx.coroutines.async

            fun test() {
                <selection>doWork()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("async")!!,
        )

        val importCount =
            myFixture.file.text
                .lines()
                .count { it.trim() == "import kotlinx.coroutines.async" }
        assertEquals("Import should appear exactly once", 1, importCount)
    }

    fun `test wrap does not duplicate runBlocking import when already present`() {
        myFixture.configureByText(
            "Test.kt",
            """
            import kotlinx.coroutines.runBlocking

            fun main() {
                <selection>suspendFun()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("runBlocking")!!,
        )

        val importCount =
            myFixture.file.text
                .lines()
                .count { it.trim() == "import kotlinx.coroutines.runBlocking" }
        assertEquals("Import should appear exactly once", 1, importCount)
    }

    fun `test wrap preserves outer indentation for deeply nested selection`() {
        myFixture.configureByText(
            "Test.kt",
            """
            class Foo {
                fun test() {
                    <selection>doWork()</selection>
                }
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("launch")!!,
        )

        myFixture.checkResult(
            """
            import kotlinx.coroutines.launch

            class Foo {
                fun test() {
                    launch {
                        doWork()
                    }
                }
            }
            """.trimIndent(),
        )
    }

    fun `test wrap does nothing when no selection`() {
        val originalCode = "fun test() {\n    doWork()\n}"
        myFixture.configureByText("Test.kt", originalCode)

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("launch")!!,
        )

        assertEquals(originalCode, myFixture.file.text)
    }

    fun `test wrap adds import to file with no existing imports`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>doWork()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("launch")!!,
        )

        assertTrue(myFixture.file.text.contains("import kotlinx.coroutines.launch"))
    }

    fun `test wrap adds withTimeout import to file with no existing imports`() {
        myFixture.configureByText(
            "Test.kt",
            """
            fun test() {
                <selection>networkCall()</selection>
            }
            """.trimIndent(),
        )

        WrapSelectionEngine.wrap(
            myFixture.project,
            myFixture.editor,
            myFixture.file as org.jetbrains.kotlin.psi.KtFile,
            CoroutineWrapperRegistry.findById("withTimeout")!!,
        )

        assertTrue(myFixture.file.text.contains("import kotlinx.coroutines.withTimeout"))
    }
}
