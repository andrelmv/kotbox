package com.github.andrelmv.kotbox.proto.generator

import com.intellij.openapi.application.ReadAction
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.Callable

/**
 * Base class for proto-generator tests.
 *
 * Runs tests off the UI thread so they can block waiting for indexing to finish.
 * Use [inSmartReadAction] to wrap any code that needs fully resolved types —
 * it waits for indexes to be ready before running.
 */
internal abstract class ProtoGeneratorTestCase : BasePlatformTestCase() {
    override fun runInDispatchThread(): Boolean = false

    fun <T> inSmartReadAction(block: () -> T): T {
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return ReadAction
            .nonBlocking(Callable { block() })
            .inSmartMode(project)
            .executeSynchronously()
    }

    fun KtFile.findClass(name: String): KtClass =
        children
            .filterIsInstance<KtClass>()
            .firstOrNull { it.name == name }
            ?: error("Class '$name' not found in ${this.name}")
}
