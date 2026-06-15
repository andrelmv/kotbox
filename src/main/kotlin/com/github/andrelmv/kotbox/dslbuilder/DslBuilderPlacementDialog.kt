package com.github.andrelmv.kotbox.dslbuilder

import com.github.andrelmv.kotbox.dslbuilder.placement.DslBuilderPlacementStrategy
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.psi.KtClass
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DslBuilderPlacementDialog(
    project: Project,
    targetClass: KtClass,
) : DialogWrapper(project, true) {
    private val sameFileButton = JBRadioButton("Same file", true)
    private val newFileButton = JBRadioButton("New file")
    private val newFileNameField = JBTextField("${targetClass.name}Builder", 20)

    init {
        title = "Generate DSL Builder"

        val group = ButtonGroup()
        group.add(sameFileButton)
        group.add(newFileButton)

        newFileNameField.isEnabled = false
        newFileButton.addChangeListener { newFileNameField.isEnabled = newFileButton.isSelected }

        init()
    }

    override fun doValidate(): ValidationInfo? {
        if (newFileButton.isSelected && newFileNameField.text.trim().isEmpty()) {
            return ValidationInfo("File name cannot be empty", newFileNameField)
        }
        return null
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc =
            GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(4)
            }

        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(sameFileButton, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(newFileButton, gbc)

        gbc.gridx = 1
        gbc.gridy = 1
        panel.add(newFileNameField, gbc)

        gbc.gridx = 2
        gbc.gridy = 1
        panel.add(JLabel(".kt"), gbc)

        return panel
    }

    fun getPlacementStrategy(): DslBuilderPlacementStrategy =
        when {
            newFileButton.isSelected -> DslBuilderPlacementStrategy.NewFile(newFileNameField.text.trim())
            else -> DslBuilderPlacementStrategy.SameFile
        }
}
