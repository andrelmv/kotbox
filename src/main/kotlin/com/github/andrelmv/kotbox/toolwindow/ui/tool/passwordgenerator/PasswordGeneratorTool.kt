package com.github.andrelmv.kotbox.toolwindow.ui.tool.passwordgenerator

import com.github.andrelmv.kotbox.services.password.PasswordConfig
import com.github.andrelmv.kotbox.services.password.PasswordGeneratorService
import com.github.andrelmv.kotbox.services.password.PasswordType
import com.github.andrelmv.kotbox.toolwindow.ui.common.IconButton
import com.github.andrelmv.kotbox.toolwindow.ui.common.editor.EditorLanguage
import com.github.andrelmv.kotbox.toolwindow.ui.common.editor.ToolEditor
import com.github.andrelmv.kotbox.toolwindow.ui.tool.base.DeveloperTool
import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.TopGap
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants

class PasswordGeneratorTool(
    project: Project,
) : DeveloperTool(project) {
    override val displayName = "Password Generator"

    // --- Config state ---
    private var selectedType = PasswordType.RANDOM

    // --- Type selector buttons ---
    private val randomButton = JButton(PasswordType.RANDOM.label)
    private val memorableButton = JButton(PasswordType.MEMORABLE.label)
    private val pinButton = JButton(PasswordType.PIN.label)
    private val typeButtons get() = listOf(randomButton, memorableButton, pinButton)

    // --- Options ---
    private val lengthSlider = JSlider(4, 64, 20)
    private val lengthLabel = JBLabel("20")
    private val numbersCheckBox = JBCheckBox("Numbers", true)
    private val symbolsCheckBox = JBCheckBox("Symbols", true)

    // --- Single password output ---
    private val passwordField =
        JTextField("").apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.BOLD, 16)
            horizontalAlignment = SwingConstants.CENTER
            preferredSize = Dimension(0, 40)
        }
    private val copyIconButton =
        IconButton(AllIcons.Actions.Copy, "Copy password") {
            val text = passwordField.text
            if (text.isNotEmpty()) CopyPasteManager.getInstance().setContents(StringSelection(text))
        }
    private val refreshButton = JButton("Regenerate")

    // --- Bulk section ---
    private val bulkSpinner = JSpinner(SpinnerNumberModel(10, 1, 1000, 1))
    private val generateBulkButton = JButton("Generate")
    private val bulkEditor =
        ToolEditor(
            project = project,
            language = EditorLanguage.PLAIN_TEXT,
            preferredLines = 10,
            showToolbar = true,
            softWraps = false,
            parentDisposable = this,
        )

    override fun Panel.buildUi() {
        // ── Type selector ────────────────────────────────────────────────────
        row { label("Choose password type").bold() }
        row {
            cell(randomButton)
            cell(memorableButton)
            cell(pinButton)
        }

        // ── Options ──────────────────────────────────────────────────────────
        row { label("Customize your new password").bold() }.topGap(TopGap.MEDIUM)
        row {
            label("Characters")
            cell(lengthSlider).align(Align.FILL)
            cell(lengthLabel)
        }
        row {
            cell(numbersCheckBox)
            cell(symbolsCheckBox)
        }

        // ── Generated password ───────────────────────────────────────────────
        row { label("Generated password").bold() }.topGap(TopGap.MEDIUM)
        val passwordFieldWithCopy =
            JPanel(BorderLayout()).apply {
                add(passwordField, BorderLayout.CENTER)
                add(copyIconButton.component, BorderLayout.EAST)
            }
        row { cell(passwordFieldWithCopy).align(Align.FILL) }
        row { cell(refreshButton) }

        // ── Bulk generation ──────────────────────────────────────────────────
        row { label("Bulk generation").bold() }.topGap(TopGap.MEDIUM)
        separator()
        row {
            label("Quantity")
            cell(bulkSpinner)
            cell(generateBulkButton)
        }.topGap(TopGap.SMALL)
        row { cell(bulkEditor.component).align(Align.FILL) }.resizableRow()

        row { }.bottomGap(BottomGap.MEDIUM)
    }

    override fun afterBuildUi() {
        setupTypeButtons()
        setupListeners()
        refreshPassword()
    }

    private fun setupTypeButtons() {
        updateTypeButtonStyles()
    }

    private fun setupListeners() {
        randomButton.addActionListener { selectType(PasswordType.RANDOM) }
        memorableButton.addActionListener { selectType(PasswordType.MEMORABLE) }
        pinButton.addActionListener { selectType(PasswordType.PIN) }

        lengthSlider.addChangeListener {
            lengthLabel.text = lengthSlider.value.toString()
            refreshPassword()
        }

        numbersCheckBox.addActionListener { refreshPassword() }
        symbolsCheckBox.addActionListener { refreshPassword() }

        refreshButton.addActionListener { refreshPassword() }

        generateBulkButton.addActionListener { generateBulk() }
    }

    private fun selectType(type: PasswordType) {
        selectedType = type
        val isPIN = type == PasswordType.PIN
        numbersCheckBox.isEnabled = !isPIN
        symbolsCheckBox.isEnabled = !isPIN
        updateTypeButtonStyles()
        refreshPassword()
    }

    private fun updateTypeButtonStyles() {
        typeButtons.forEach { it.putClientProperty("JButton.buttonType", "default") }
        val active =
            when (selectedType) {
                PasswordType.RANDOM -> randomButton
                PasswordType.MEMORABLE -> memorableButton
                PasswordType.PIN -> pinButton
            }
        // Visually distinguish the selected button using font weight
        typeButtons.forEach { btn ->
            btn.font = btn.font.deriveFont(if (btn == active) Font.BOLD else Font.PLAIN)
        }
    }

    private fun currentConfig() =
        PasswordConfig(
            type = selectedType,
            length = lengthSlider.value,
            includeNumbers = numbersCheckBox.isSelected && numbersCheckBox.isEnabled,
            includeSymbols = symbolsCheckBox.isSelected && symbolsCheckBox.isEnabled,
        )

    private fun refreshPassword() {
        passwordField.text = PasswordGeneratorService.generate(currentConfig())
    }

    private fun generateBulk() {
        val count = (bulkSpinner.value as Int).coerceIn(1, 1000)
        val passwords = PasswordGeneratorService.generateBulk(currentConfig(), count)
        bulkEditor.text = passwords.joinToString("\n")
    }

    override fun activated() {
        if (passwordField.text.isEmpty()) refreshPassword()
    }

    override fun reset() {
        selectedType = PasswordType.RANDOM
        lengthSlider.value = 20
        numbersCheckBox.isSelected = true
        numbersCheckBox.isEnabled = true
        symbolsCheckBox.isSelected = true
        symbolsCheckBox.isEnabled = true
        bulkEditor.text = ""
        updateTypeButtonStyles()
        refreshPassword()
    }
}
