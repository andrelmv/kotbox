package com.github.andrelmv.kotbox.inlay

import com.github.andrelmv.kotbox.inlay.config.InlayStringInterpolationProvider
import com.github.andrelmv.kotbox.inlay.config.InlayStringInterpolationSettings
import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase

@Suppress("UnstableApiUsage")
class InlayStringInterpolationTest : InlayHintsProviderTestCase() {
    override fun runInDispatchThread(): Boolean {
        return true
    }

    fun `test given a const interpolated string when evaluating hints then return inlay hint`() {
        val text =
            "const val NAME = \"André\" \n" +
                "const val SURNAME = \"Monteiro\" \n" +
                "const val FULL_NAME = \"\$NAME \$SURNAME/*<# André Monteiro #>*/\""

        testInlayHint(text)
    }

    fun `test given a const not interpolated string when evaluating hints then return no inlay hint`() {
        val text = "const val NAME = \"André\""

        testInlayHint(text)
    }

    fun `test given a string const in a constructor when evaluating hints then return inlay hint`() {
        val text =
            "const val NAME = \"André\" \n" +
                "data class User(name: String) \n" +
                "val user =  User(name = NAME/*<# André #>*/)"

        testInlayHint(text)
    }

    fun `test given a integer in a constructor when evaluating hints then return no inlay hint`() {
        val text =
            "data class User(age: Int) \n" +
                "val user =  User(age = 0)"

        testInlayHint(text)
    }

    private fun testInlayHint(text: String) {
        doTestProvider(
            fileName = "Test.kt",
            expectedText = text,
            provider = InlayStringInterpolationProvider(),
            settings = InlayStringInterpolationSettings(),
            verifyHintPresence = false,
        )
    }
}
