package com.github.andrelmv.kotbox.inlay

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.util.startOffset
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert
import java.awt.datatransfer.DataFlavor

class CopyInterpolatedValueIntentionTest : BasePlatformTestCase() {
    fun `test interpolated string with two variable references`() {
        val text =
            "  const val NAME = \"Name\" \n" +
                "const val SURNAME = \"Last Name\" \n" +
                "const val FULL_NAME = \"\$NAME \$SURNAME\""

        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(myFixture.file.lastChild.startOffset)

        val action = myFixture.findSingleIntention("Copy interpolated value")
        Assert.assertNotNull(action)
        myFixture.launchAction(action)

        val data = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        Assert.assertEquals("Name Last Name", data)
    }

    fun `test intention not available for plain string with no interpolation`() {
        val text = "const val GREETING = \"Hello, World!\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"Hello"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test intention not available for string with escape sequences only`() {
        val text = "const val MSG = \"line1\\nline2\\ttabbed\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"line1"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test intention not available for unresolvable variable reference`() {
        val text = "const val MSG = \"Hello \$UNKNOWN\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"Hello"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test nested interpolated string`() {
        val text =
            "const val FIRST = \"John\" \n" +
                "const val LAST = \"Doe\" \n" +
                "const val FULL = \"\$FIRST \$LAST\" \n" +
                "const val MSG = \"Name: \$FULL\""
        val data = launchIntention(text, text.indexOf("\"Name:"))
        Assert.assertEquals("Name: John Doe", data)
    }

    fun `test intention not available on non-string property`() {
        val text = "const val NUMBER = 42"
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("42"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test intention not available outside a property`() {
        val text = "fun main() { println(\"hello\") }"
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("fun"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test intention not available on property without initializer`() {
        val text = "val name: String"
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("name"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test isAvailable returns false when findElementAt returns null`() {
        val text = "const val A = \"hello\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.length)
        val result = CopyInterpolatedValueIntention().isAvailable(myFixture.project, myFixture.editor, myFixture.file)
        Assert.assertFalse(result)
    }

    fun `test isAvailable returns false when editor is null`() {
        myFixture.configureByText("test.kt", "const val NAME = \"John\"")
        val result = CopyInterpolatedValueIntention().isAvailable(myFixture.project, null, myFixture.file)
        Assert.assertFalse(result)
    }

    fun `test isAvailable returns false when file is not a KtFile`() {
        myFixture.configureByText("test.txt", "const val NAME = \"John\"")
        val result = CopyInterpolatedValueIntention().isAvailable(myFixture.project, myFixture.editor, myFixture.file)
        Assert.assertFalse(result)
    }

    fun `test invoke does nothing when editor is null`() {
        myFixture.configureByText("test.kt", "const val NAME = \"John\"")
        CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection("unchanged"))
        CopyInterpolatedValueIntention().invoke(myFixture.project, null, myFixture.file)
        val data = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        Assert.assertEquals("unchanged", data)
    }

    fun `test invoke does nothing when file is not a KtFile`() {
        myFixture.configureByText("test.txt", "const val NAME = \"John\"")
        CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection("unchanged"))
        CopyInterpolatedValueIntention().invoke(myFixture.project, myFixture.editor, myFixture.file)
        val data = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        Assert.assertEquals("unchanged", data)
    }

    fun `test invoke does nothing when caret is at end of file`() {
        val text = "const val A = \"hello\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.length)
        CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection("unchanged"))
        CopyInterpolatedValueIntention().invoke(myFixture.project, myFixture.editor, myFixture.file)
        val data = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        Assert.assertEquals("unchanged", data)
    }

    fun `test invoke does nothing when property has no initializer`() {
        val text = "val name: String"
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("name"))
        CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection("unchanged"))
        CopyInterpolatedValueIntention().invoke(myFixture.project, myFixture.editor, myFixture.file)
        val data = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        Assert.assertEquals("unchanged", data)
    }

    fun `test invoke does nothing when expression is not evaluable`() {
        val text = "const val MSG = \"Hello \$UNKNOWN\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"Hello"))
        CopyPasteManager.getInstance().setContents(java.awt.datatransfer.StringSelection("unchanged"))
        CopyInterpolatedValueIntention().invoke(myFixture.project, myFixture.editor, myFixture.file)
        val data = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        Assert.assertEquals("unchanged", data)
    }

    fun `test intention not available for empty string`() {
        val text = "const val EMPTY = \"\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"\""))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test intention not available for string with embedded quotes only`() {
        val text = "const val QUOTED = \"\\\"hello\\\"\""
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"\\\""))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    fun `test intention not available for local variable inside function body`() {
        val text = "fun main() { val x = \"hello\" }"
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(text.indexOf("\"hello"))
        val intentions = myFixture.filterAvailableIntentions("Copy interpolated value")
        Assert.assertTrue(intentions.isEmpty())
    }

    private fun launchIntention(
        text: String,
        offset: Int,
    ): String? {
        myFixture.configureByText("test.kt", text)
        myFixture.editor.caretModel.moveToOffset(offset)
        val action = myFixture.findSingleIntention("Copy interpolated value")
        Assert.assertNotNull(action)
        myFixture.launchAction(action)
        return CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
    }
}
