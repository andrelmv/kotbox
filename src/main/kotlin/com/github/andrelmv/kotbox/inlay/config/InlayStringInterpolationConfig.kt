package com.github.andrelmv.kotbox.inlay.config

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBEmptyBorder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class InlayStringInterpolationConfig(
    private val inlayStringInterpolationSettings: InlayStringInterpolationSettings,
) : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        val border = JBEmptyBorder(5, 0, 5, 0)

        val title = JLabel("Kotlin Toolbox - Inlay Hints")
        title.border = border

        val withStringInterpolationHint = JCheckBox()
        withStringInterpolationHint.border = border
        withStringInterpolationHint.isSelected = inlayStringInterpolationSettings.state.withStringInterpolationHint
        withStringInterpolationHint.addChangeListener {
            inlayStringInterpolationSettings.state.withStringInterpolationHint = withStringInterpolationHint.isSelected
            listener.settingsChanged()
        }

        val withStringConstantHint = JCheckBox()
        withStringConstantHint.border = border
        withStringConstantHint.isSelected = inlayStringInterpolationSettings.state.withStringConstantHint
        withStringConstantHint.addChangeListener {
            inlayStringInterpolationSettings.state.withStringConstantHint = withStringConstantHint.isSelected
            listener.settingsChanged()
        }

        withStringInterpolationHint.addChangeListener { listener.settingsChanged() }
        withStringConstantHint.addChangeListener { listener.settingsChanged() }

        return FormBuilder
            .createFormBuilder()
            .addComponent(title)
            .addSeparator()
            .addLabeledComponent(STRING_CONSTANT_INTERPOLATION_INLAY_HINT, withStringInterpolationHint)
            .addLabeledComponent(STRING_CONSTANT_INLAY_HINT, withStringConstantHint)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}

private const val STRING_CONSTANT_INTERPOLATION_INLAY_HINT = "String Constant Interpolation Inlay Hints"
private const val STRING_CONSTANT_INLAY_HINT = "String Constant Inlay Hints"
