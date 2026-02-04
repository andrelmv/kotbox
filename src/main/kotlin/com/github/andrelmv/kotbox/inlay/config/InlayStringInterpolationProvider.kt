package com.github.andrelmv.kotbox.inlay.config

import com.github.andrelmv.kotbox.inlay.InlayStringInterpolationHintCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage

@Suppress("UnstableApiUsage")
class InlayStringInterpolationProvider : InlayHintsProvider<InlayStringInterpolationSettings> {
    private val settingsKey: SettingsKey<InlayStringInterpolationSettings> = SettingsKey(STR_INTERPOLATION_HINT)

    override val key: SettingsKey<InlayStringInterpolationSettings>
        get() = settingsKey

    override val name: String
        get() = STR_INTERPOLATION_HINT

    override val previewText: String
        get() = """
            const val NAME = "André"
            const val SURNAME = "Monteiro"
            const val FULL_NAME = "${'$'}NAME ${'$'}SURNAME"

            data class User(
                val name: String,
                val surname: String,
            )

            val user = User(
                name = NAME,
                surname = SURNAME,
            )
            """

    override fun createConfigurable(settings: InlayStringInterpolationSettings): ImmediateConfigurable =
        InlayStringInterpolationConfig(settings)

    override fun isLanguageSupported(language: Language): Boolean = language == KotlinLanguage.INSTANCE

    override fun createSettings(): InlayStringInterpolationSettings = service<InlayStringInterpolationSettings>()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: InlayStringInterpolationSettings,
        sink: InlayHintsSink,
    ): InlayHintsCollector = InlayStringInterpolationHintCollector(settings, editor)
}

private const val STR_INTERPOLATION_HINT = "Kotlin Toolbox"
