package com.github.andrelmv.kotbox.toolwindow.ui.tool.jwt

import com.github.andrelmv.kotbox.services.token.JwtService
import com.github.andrelmv.kotbox.toolwindow.ui.common.editor.EditorLanguage
import com.github.andrelmv.kotbox.toolwindow.ui.common.editor.ToolEditor
import com.github.andrelmv.kotbox.toolwindow.ui.tool.base.DeveloperTool
import com.github.andrelmv.kotbox.utils.formatJson
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.TopGap
import javax.swing.JButton

class JwtEncoderDecoder(
    project: Project,
) : DeveloperTool(project) {
    override val displayName: String = "JWT Encoder/Decoder"

    // State
    private var liveConversion = true
    private var updating = false

    // Editors
    private val encodedEditor =
        ToolEditor(
            project = project,
            language = EditorLanguage.PLAIN_TEXT,
            preferredLines = 6,
            parentDisposable = this,
        )
    private val headerEditor =
        ToolEditor(
            project = project,
            language = EditorLanguage.JSON,
            preferredLines = 6,
            parentDisposable = this,
        )
    private val payloadEditor =
        ToolEditor(
            project = project,
            language = EditorLanguage.JSON,
            preferredLines = 12,
            parentDisposable = this,
        )

    // Controls
    private val validSignatureLabel = JBLabel("Valid signature", AllIcons.General.InspectionsOK, JBLabel.LEFT)
    private val liveConversionCheckBox = JBCheckBox("Live conversion", true)
    private val decodeButton = JButton("\u25BC Decode")
    private val encodeButton = JButton("\u25B2 Encode")

    // Signature configuration
    private val signatureConfigPanel = SignatureConfigPanel { onSignatureConfigChanged() }

    override fun Panel.buildUi() {
        row { label("Encoded token:").bold() }
        row { cell(encodedEditor.component).align(Align.FILL) }.resizableRow()
        row { cell(validSignatureLabel) }

        row {
            cell(liveConversionCheckBox)
            cell(decodeButton)
            cell(encodeButton)
        }

        row { label("Header (JSON):").bold() }.topGap(TopGap.MEDIUM)
        row { cell(headerEditor.component).align(Align.FILL) }.resizableRow()

        row { label("Payload (JSON):").bold() }.topGap(TopGap.SMALL)
        row { cell(payloadEditor.component).align(Align.FILL) }.resizableRow()

        signatureConfigPanel.buildUi(this)

        row { }.bottomGap(BottomGap.MEDIUM)
    }

    override fun afterBuildUi() {
        setupListeners()
        loadExample()
    }

    private fun setupListeners() {
        liveConversionCheckBox.addActionListener {
            liveConversion = liveConversionCheckBox.isSelected
            decodeButton.isEnabled = !liveConversion
            encodeButton.isEnabled = !liveConversion
            if (liveConversion) decode()
        }
        decodeButton.isEnabled = false
        encodeButton.isEnabled = false

        decodeButton.addActionListener { decode() }
        encodeButton.addActionListener { encode() }

        encodedEditor.onTextChanged { onEncodedChanged() }
        headerEditor.onTextChanged { onHeaderPayloadChanged() }
        payloadEditor.onTextChanged { onHeaderPayloadChanged() }

        signatureConfigPanel.setupListeners()
    }

    private fun onEncodedChanged() {
        if (!updating && liveConversion) decode()
    }

    private fun onHeaderPayloadChanged() {
        if (!updating && liveConversion) encode()
    }

    private fun onSignatureConfigChanged() {
        if (!updating && liveConversion && encodedEditor.text.trim().isNotEmpty()) {
            decode()
        }
    }

    private fun decode() {
        val encoded = encodedEditor.text.trim()
        if (encoded.isEmpty()) {
            updateWithoutTrigger {
                headerEditor.text = ""
                payloadEditor.text = ""
                updateSignatureStatus(false)
            }
            return
        }

        try {
            val result = JwtService.decodeAndVerify(encoded, signatureConfigPanel.currentSigningConfig())
            updateWithoutTrigger {
                headerEditor.text = result.header
                payloadEditor.text = result.payload
            }
            updateSignatureStatus(result.isSignatureValid)
        } catch (e: Exception) {
            updateWithoutTrigger {
                headerEditor.text = "Error: ${e.message}"
                payloadEditor.text = ""
                updateSignatureStatus(false)
            }
        }
    }

    private fun encode() {
        val headerText = headerEditor.text.trim()
        val payloadText = payloadEditor.text.trim()
        if (headerText.isEmpty() || payloadText.isEmpty()) return

        try {
            val result = JwtService.encode(headerText, payloadText, signatureConfigPanel.currentSigningConfig())
            updateWithoutTrigger {
                encodedEditor.text = result.encodedToken
                updateSignatureStatus(true)
            }
        } catch (_: Exception) {
            updateWithoutTrigger {
                updateSignatureStatus(false)
            }
        }
    }

    private fun updateWithoutTrigger(block: () -> Unit) {
        updating = true
        try {
            block()
        } finally {
            updating = false
        }
    }

    private fun loadExample() {
        val exampleHeader = """{"typ":"JWT","alg":"HS256"}"""
        val examplePayload = """{"jti":"96492d59-8ad5-4c08-892d-590ad5ac00f3","sub":"0123456789","name":"John Doe","iat":1661040015}"""

        val config = signatureConfigPanel.currentSigningConfig()
        val result = JwtService.encode(exampleHeader, examplePayload, config)

        updating = true
        encodedEditor.text = result.encodedToken
        headerEditor.text = formatJson(exampleHeader)
        payloadEditor.text = formatJson(examplePayload)
        updateSignatureStatus(true)
        updating = false
    }

    private fun updateSignatureStatus(isValid: Boolean) {
        if (isValid) {
            validSignatureLabel.icon = AllIcons.General.InspectionsOK
            validSignatureLabel.text = "Valid signature"
        } else {
            validSignatureLabel.icon = AllIcons.General.Error
            validSignatureLabel.text = "Invalid signature"
        }
    }
}
