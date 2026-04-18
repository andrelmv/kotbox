package com.github.andrelmv.kotbox.intentions

import com.intellij.psi.util.startOffset
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class CopyInterpolatedValueIntentionTest : BasePlatformTestCase() {
    fun testIntention() {
        val text =
            "  const val NAME = \"Name\" \n" +
                "const val SURNAME = \"Last Name\" \n" +
                "const val FULL_NAME = \"\$NAME \$SURNAME\""

        myFixture.configureByText("test.kt", text)

        myFixture.editor.caretModel.moveToOffset(myFixture.file.lastChild.startOffset)

        val action = myFixture.findSingleIntention("Copy string value")
        Assert.assertNotNull(action)
        myFixture.launchAction(action)

        val data = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        Assert.assertEquals("Name Last Name", data)
    }
}
