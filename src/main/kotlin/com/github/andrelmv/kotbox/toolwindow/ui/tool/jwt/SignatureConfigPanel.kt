package com.github.andrelmv.kotbox.toolwindow.ui.tool.jwt

import com.github.andrelmv.kotbox.services.token.AlgorithmKind
import com.github.andrelmv.kotbox.services.token.SecretKeyEncoding
import com.github.andrelmv.kotbox.services.token.SignatureAlgorithm
import com.github.andrelmv.kotbox.services.token.SigningConfig
import com.github.andrelmv.kotbox.toolwindow.ui.common.IconButton
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SignatureConfigPanel(
    private val onConfigChanged: () -> Unit,
) {
    private val algorithmComboBox =
        ComboBox(SignatureAlgorithm.entries.toTypedArray()).apply {
            selectedItem = SignatureAlgorithm.HMAC256
        }

    private val secretKeyField =
        JTextArea(1, 0).apply {
            lineWrap = false
            font = MONOSPACE_FONT
            text = "your-secret-key"
        }

    private val privateKeyArea =
        JTextArea(4, 0).apply {
            lineWrap = true
            wrapStyleWord = true
            font = MONOSPACE_FONT
            text = "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
        }

    private val strictValidationCheckBox = JBCheckBox("Strict key requirements validation")

    private var secretKeyEncoding = SecretKeyEncoding.RAW
    private val secretKeyGearButton =
        IconButton(
            icon = AllIcons.General.GearPlain,
            tooltip = "Secret key encoding",
        ) { showSecretKeyEncodingMenu() }

    private val keyCardLayout = CardLayout()
    private val keyCardPanel = createKeyCardPanel()
    private val keyLabel = JBLabel("Secret key:")

    fun buildUi(panel: Panel) {
        panel.collapsibleGroup("Signature Algorithm Configuration") {
            row {
                label("Algorithm:")
                cell(algorithmComboBox)
            }
            row { cell(keyLabel) }
            row { cell(keyCardPanel).align(AlignX.FILL) }
            row {
                cell(strictValidationCheckBox)
            }.contextHelp(
                "Validates minimum key length requirements per RFC 7518:\n" +
                    "HS256: minimum 256 bits (32 bytes)\n" +
                    "HS384: minimum 384 bits (48 bytes)\n" +
                    "HS512: minimum 512 bits (64 bytes)",
            )
        }
    }

    fun setupListeners() {
        algorithmComboBox.addActionListener {
            val algo = algorithmComboBox.selectedItem as? SignatureAlgorithm ?: return@addActionListener
            updateKeyUI(algo)
            onConfigChanged()
        }

        secretKeyField.document.addDocumentListener(createDocumentListener { onConfigChanged() })
        privateKeyArea.document.addDocumentListener(createDocumentListener { onConfigChanged() })
        strictValidationCheckBox.addActionListener { onConfigChanged() }
    }

    fun currentSigningConfig(): SigningConfig {
        return SigningConfig(
            algorithm = algorithmComboBox.selectedItem as? SignatureAlgorithm ?: SignatureAlgorithm.HMAC256,
            secretKey = secretKeyField.text,
            privateKeyPem = privateKeyArea.text,
            strictValidation = strictValidationCheckBox.isSelected,
            secretKeyEncoding = secretKeyEncoding,
        )
    }

    private fun createKeyCardPanel(): JPanel {
        return JPanel(keyCardLayout).also { cardPanel ->
            cardPanel.add(
                JPanel(BorderLayout()).apply {
                    add(
                        JBScrollPane(secretKeyField).apply {
                            preferredSize = Dimension(0, 40)
                        },
                        BorderLayout.CENTER,
                    )
                    add(
                        JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
                            isOpaque = false
                            border = JBUI.Borders.emptyLeft(4)
                            add(secretKeyGearButton.component)
                        },
                        BorderLayout.EAST,
                    )
                },
                CARD_SECRET,
            )
            cardPanel.add(
                JBScrollPane(privateKeyArea).apply {
                    preferredSize = Dimension(0, 100)
                },
                CARD_PRIVATE_KEY,
            )
            keyCardLayout.show(cardPanel, CARD_SECRET)
        }
    }

    private fun updateKeyUI(algorithm: SignatureAlgorithm) {
        keyLabel.text = algorithm.kind.label
        if (algorithm.kind == AlgorithmKind.HMAC) {
            keyCardLayout.show(keyCardPanel, CARD_SECRET)
        } else {
            keyCardLayout.show(keyCardPanel, CARD_PRIVATE_KEY)
        }
        keyCardPanel.revalidate()
        keyCardPanel.repaint()
    }

    private fun showSecretKeyEncodingMenu() {
        val encodings = SecretKeyEncoding.entries
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(encodings.toList())
            .setRenderer { list, value, _, isSelected, _ ->
                JBLabel(value.displayName).apply {
                    border = JBUI.Borders.empty(4, 8)
                    if (value == secretKeyEncoding) {
                        icon = AllIcons.Actions.Checked
                    }
                    isOpaque = true
                    if (isSelected) {
                        background = list.selectionBackground
                        foreground = list.selectionForeground
                    }
                }
            }
            .setItemChosenCallback { selected ->
                secretKeyEncoding = selected
                onConfigChanged()
            }
            .createPopup()
            .showUnderneathOf(secretKeyGearButton.component)
    }

    private fun createDocumentListener(action: () -> Unit): DocumentListener {
        return object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = action()

            override fun removeUpdate(e: DocumentEvent) = action()

            override fun changedUpdate(e: DocumentEvent) = action()
        }
    }

    companion object {
        private const val CARD_SECRET = "secret"
        private const val CARD_PRIVATE_KEY = "privateKey"
        private val MONOSPACE_FONT = Font("Monospaced", Font.PLAIN, 14)
    }
}
