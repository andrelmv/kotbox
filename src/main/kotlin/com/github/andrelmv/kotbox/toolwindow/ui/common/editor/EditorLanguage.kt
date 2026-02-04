package com.github.andrelmv.kotbox.toolwindow.ui.common.editor

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage

enum class EditorLanguage(
    val languageId: String,
) {
    PLAIN_TEXT("TEXT"),
    JSON("JSON"),
    XML("XML"),
    HTML("HTML"),
    YAML("yaml"),
    SQL("SQL"),
    MARKDOWN("Markdown"),
    KOTLIN("kotlin"),
    JAVA("JAVA"),
    JAVASCRIPT("JavaScript"),
    ;

    fun getLanguage(): Language = Language.findLanguageByID(languageId) ?: PlainTextLanguage.INSTANCE

    companion object {
        fun plainText(): Language = Language.findLanguageByID("TEXT") ?: PlainTextLanguage.INSTANCE
    }
}
