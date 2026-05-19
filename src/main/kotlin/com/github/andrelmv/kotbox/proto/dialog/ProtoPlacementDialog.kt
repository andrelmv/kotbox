package com.github.andrelmv.kotbox.proto.dialog

import com.github.andrelmv.kotbox.proto.placement.PlacementStrategy
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
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

class ProtoPlacementDialog(
    title: String,
    project: Project,
    targetClass: KtClass,
) : DialogWrapper(project, true) {
    private val newFileButton = JBRadioButton("New file", true)
    private val previewAndCopyButton = JBRadioButton("Preview & copy")
    private val newFileNameField = JBTextField(targetClass.name, 20)

    init {
        this.title = "Generate $title"
        init()

        val group = ButtonGroup()
        group.add(newFileButton)
        group.add(previewAndCopyButton)

        newFileNameField.isEnabled = true
        previewAndCopyButton.addChangeListener {
            newFileNameField.isEnabled = newFileButton.isSelected
        }
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
        panel.add(newFileButton, gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        panel.add(newFileNameField, gbc)

        gbc.gridx = 2
        gbc.gridy = 0
        panel.add(JLabel(".proto"), gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(previewAndCopyButton, gbc)

        return panel
    }

    fun getPlacementStrategy(): PlacementStrategy =
        when {
            newFileButton.isSelected -> PlacementStrategy.NewFile(newFileNameField.text.trim())
            else -> PlacementStrategy.PreviewAndCopy
        }
}
