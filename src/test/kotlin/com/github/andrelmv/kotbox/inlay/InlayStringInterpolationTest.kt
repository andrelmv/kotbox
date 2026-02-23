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

    fun `test given const string templates concatenated when evaluating hints then return hint for each`() {
        val text =
            "const val FIRST = \"Hello\" \n" +
                "const val SECOND = \" World\" \n" +
                "val greeting = \"\$FIRST/*<# Hello #>*/\" + \"\$SECOND/*<#  World #>*/\""

        testInlayHint(text)
    }

    fun `test given const template concatenated with plain string when evaluating hints then return hint only for interpolated`() {
        val text =
            "const val NAME = \"André\" \n" +
                "val greeting = \"\$NAME/*<# André #>*/\" + \" test\""

        testInlayHint(text)
    }

    fun `test given a const in binary expression when evaluating hints then return inlay hint`() {
        val text =
            "const val NAME = \"André\" \n" +
                "val result = NAME/*<# André #>*/ + \" test\""

        testInlayHint(text)
    }

    fun `test given a const as expression function body when evaluating hints then return inlay hint`() {
        val text =
            "const val NAME = \"André\" \n" +
                "fun getName() = NAME/*<# André #>*/"

        testInlayHint(text)
    }

    fun `test given a non-constant interpolated string when evaluating hints then return no hint`() {
        val text =
            "val name = \"André\" \n" +
                "val greeting = \"\$name\""

        testInlayHint(text)
    }

    fun `test given interpolation hints disabled when evaluating hints then return no hint for string template`() {
        val text =
            "const val NAME = \"André\" \n" +
                "const val FULL_NAME = \"\$NAME test\""

        val settings = InlayStringInterpolationSettings()
        settings.state.withStringInterpolationHint = false
        testInlayHint(text, settings)
    }

    fun `test given interpolation hints disabled when evaluating hints then return no hint for concatenation`() {
        val text =
            "const val FIRST = \"Hello\" \n" +
                "const val SECOND = \" World\" \n" +
                "val greeting = \"\$FIRST\" + \"\$SECOND\""

        val settings = InlayStringInterpolationSettings()
        settings.state.withStringInterpolationHint = false
        testInlayHint(text, settings)
    }

    fun `test given three const string templates concatenated when evaluating hints then return hint for each`() {
        val text =
            "const val A = \"Hello\" \n" +
                "const val B = \"World\" \n" +
                "val message = \"\$A/*<# Hello #>*/\" + \"!\" + \"\$B/*<# World #>*/\""

        testInlayHint(text)
    }

    fun `test given constant hints disabled when evaluating hints then return no hint for name reference`() {
        val text =
            "const val NAME = \"André\" \n" +
                "data class User(name: String) \n" +
                "val user = User(name = NAME)"

        val settings = InlayStringInterpolationSettings()
        settings.state.withStringConstantHint = false
        testInlayHint(text, settings)
    }

    private fun testInlayHint(
        text: String,
        settings: InlayStringInterpolationSettings = InlayStringInterpolationSettings(),
    ) {
        doTestProvider(
            fileName = "Test.kt",
            expectedText = text,
            provider = InlayStringInterpolationProvider(),
            settings = settings,
            verifyHintPresence = false,
        )
    }
}
