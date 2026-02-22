package com.github.andrelmv.kotbox.services.token

import com.github.andrelmv.kotbox.utils.compactJson
import com.github.andrelmv.kotbox.utils.formatJson
import com.intellij.openapi.diagnostic.thisLogger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object JwtService {
    private val LOG = thisLogger()

    fun decode(
        encodedToken: String,
        config: SigningConfig,
    ): DecodeResult {
        val parts = encodedToken.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT: expected 3 parts separated by '.', found ${parts.size}")
        }

        val headerJson = decodeBase64Url(parts[0])
        val payloadJson = decodeBase64Url(parts[1])

        return DecodeResult(
            header = formatJson(headerJson),
            payload = formatJson(payloadJson),
            encodedSignature = parts[2],
        )
    }

    fun decodeAndVerify(
        encodedToken: String,
        config: SigningConfig,
    ): DecodeResult {
        val result = decode(encodedToken, config)
        val parts = encodedToken.split(".")
        val isValid =
            try {
                verifySignature(parts[0], parts[1], result.encodedSignature, config)
            } catch (e: Exception) {
                LOG.debug("Signature verification failed", e)
                false
            }
        return result.copy(isSignatureValid = isValid)
    }

    fun encode(
        headerJson: String,
        payloadJson: String,
        config: SigningConfig,
    ): EncodeResult {
        if (!validateHmacKeyLength(config)) {
            val keyBytes = resolveSecretKeyBytes(config).size
            throw IllegalArgumentException(
                "HMAC key too short: $keyBytes bytes, minimum ${config.algorithm.minKeyBytes} required",
            )
        }

        val headerCompact = compactJson(headerJson)
        val payloadCompact = compactJson(payloadJson)

        val encodedHeader = encodeBase64Url(headerCompact)
        val encodedPayload = encodeBase64Url(payloadCompact)
        val signature = signPayload(encodedHeader, encodedPayload, config)

        return EncodeResult(
            encodedToken = "$encodedHeader.$encodedPayload.$signature",
            encodedHeader = encodedHeader,
            encodedPayload = encodedPayload,
            encodedSignature = signature,
        )
    }

    fun signPayload(
        encodedHeader: String,
        encodedPayload: String,
        config: SigningConfig,
    ): String {
        val algorithm = config.algorithm
        val data = "$encodedHeader.$encodedPayload".toByteArray(Charsets.UTF_8)

        return when (algorithm.kind) {
            AlgorithmKind.HMAC -> {
                val mac = Mac.getInstance(algorithm.jdkAlgorithm)
                mac.init(SecretKeySpec(resolveSecretKeyBytes(config), algorithm.jdkAlgorithm))
                Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data))
            }

            AlgorithmKind.RSA, AlgorithmKind.RSA_PSS -> {
                val privateKey = parsePKCS8PrivateKey(config.privateKeyPem, "RSA")
                val sig = Signature.getInstance(algorithm.jdkAlgorithm)
                if (algorithm.kind == AlgorithmKind.RSA_PSS) {
                    sig.setParameter(getPSSParameterSpec(algorithm))
                }
                sig.initSign(privateKey)
                sig.update(data)
                Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign())
            }

            AlgorithmKind.ECDSA -> {
                val privateKey = parsePKCS8PrivateKey(config.privateKeyPem, "EC")
                val sig = Signature.getInstance(algorithm.jdkAlgorithm)
                sig.initSign(privateKey)
                sig.update(data)
                val derSignature = sig.sign()
                val rawSignature = derToRawEcdsa(derSignature, algorithm.ecComponentSize)
                Base64.getUrlEncoder().withoutPadding().encodeToString(rawSignature)
            }

            AlgorithmKind.EDDSA -> {
                val privateKey = parsePKCS8PrivateKey(config.privateKeyPem, "Ed25519")
                val sig = Signature.getInstance(algorithm.jdkAlgorithm)
                sig.initSign(privateKey)
                sig.update(data)
                Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign())
            }
        }
    }

    fun verifySignature(
        encodedHeader: String,
        encodedPayload: String,
        encodedSignature: String,
        config: SigningConfig,
    ): Boolean {
        val algorithm = config.algorithm

        if (!validateHmacKeyLength(config)) return false

        val data = "$encodedHeader.$encodedPayload".toByteArray(Charsets.UTF_8)
        val signatureBytes = Base64.getUrlDecoder().decode(encodedSignature)

        return when (algorithm.kind) {
            AlgorithmKind.HMAC -> {
                val mac = Mac.getInstance(algorithm.jdkAlgorithm)
                mac.init(SecretKeySpec(resolveSecretKeyBytes(config), algorithm.jdkAlgorithm))
                mac.doFinal(data).contentEquals(signatureBytes)
            }

            AlgorithmKind.RSA, AlgorithmKind.RSA_PSS -> {
                val privateKey = parsePKCS8PrivateKey(config.privateKeyPem, "RSA") as RSAPrivateCrtKey
                val publicKey =
                    KeyFactory.getInstance("RSA")
                        .generatePublic(RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent))
                val sig = Signature.getInstance(algorithm.jdkAlgorithm)
                if (algorithm.kind == AlgorithmKind.RSA_PSS) {
                    sig.setParameter(getPSSParameterSpec(algorithm))
                }
                sig.initVerify(publicKey)
                sig.update(data)
                sig.verify(signatureBytes)
            }

            AlgorithmKind.ECDSA -> {
                // Re-sign and compare; ECDSA is non-deterministic so this only validates
                // tokens that were signed within this session
                val expected = signPayload(encodedHeader, encodedPayload, config)
                expected == encodedSignature
            }

            AlgorithmKind.EDDSA -> {
                // EdDSA is deterministic, re-sign and compare works correctly
                val expected = signPayload(encodedHeader, encodedPayload, config)
                expected == encodedSignature
            }
        }
    }

    fun parsePKCS8PrivateKey(
        pem: String,
        algorithm: String,
    ): PrivateKey {
        val base64 =
            pem.trim()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(base64)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance(algorithm).generatePrivate(keySpec)
    }

    fun derToRawEcdsa(
        der: ByteArray,
        componentSize: Int,
    ): ByteArray {
        if (der.size < 6) throw IllegalArgumentException("DER signature too short: ${der.size} bytes")
        // DER: 0x30 <totalLen> 0x02 <rLen> <r> 0x02 <sLen> <s>
        var offset = 2 // skip SEQUENCE tag and length
        if (der[1].toInt() and 0x80 != 0) {
            offset += (der[1].toInt() and 0x7F) // long form length
        }

        // Read R
        if (der[offset++].toInt() != 0x02) throw IllegalArgumentException("Expected INTEGER tag for R")
        val rLen = der[offset++].toInt() and 0xFF
        val rBytes = der.copyOfRange(offset, offset + rLen)
        offset += rLen

        // Read S
        if (der[offset++].toInt() != 0x02) throw IllegalArgumentException("Expected INTEGER tag for S")
        val sLen = der[offset++].toInt() and 0xFF
        val sBytes = der.copyOfRange(offset, offset + sLen)

        val result = ByteArray(componentSize * 2)
        copyWithPadding(rBytes, result, 0, componentSize)
        copyWithPadding(sBytes, result, componentSize, componentSize)
        return result
    }

    fun getPSSParameterSpec(algorithm: SignatureAlgorithm): PSSParameterSpec {
        return when (algorithm) {
            SignatureAlgorithm.PS256 ->
                PSSParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    32,
                    PSSParameterSpec.TRAILER_FIELD_BC,
                )

            SignatureAlgorithm.PS384 ->
                PSSParameterSpec(
                    "SHA-384",
                    "MGF1",
                    MGF1ParameterSpec.SHA384,
                    48,
                    PSSParameterSpec.TRAILER_FIELD_BC,
                )

            SignatureAlgorithm.PS512 ->
                PSSParameterSpec(
                    "SHA-512",
                    "MGF1",
                    MGF1ParameterSpec.SHA512,
                    64,
                    PSSParameterSpec.TRAILER_FIELD_BC,
                )

            else -> throw IllegalArgumentException("Not a PSS algorithm: $algorithm")
        }
    }

    fun resolveSecretKeyBytes(config: SigningConfig): ByteArray {
        return when (config.secretKeyEncoding) {
            SecretKeyEncoding.RAW -> config.secretKey.toByteArray(Charsets.UTF_8)
            SecretKeyEncoding.BASE32 -> decodeBase32(config.secretKey)
            SecretKeyEncoding.BASE64 -> Base64.getDecoder().decode(config.secretKey)
        }
    }

    private fun validateHmacKeyLength(config: SigningConfig): Boolean {
        if (config.algorithm.kind != AlgorithmKind.HMAC || !config.strictValidation) return true
        return resolveSecretKeyBytes(config).size >= config.algorithm.minKeyBytes
    }

    private fun decodeBase32(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = input.uppercase().replace("=", "").replace("\\s".toRegex(), "")
        val bits = StringBuilder()
        for (c in cleaned) {
            val value = alphabet.indexOf(c)
            if (value < 0) throw IllegalArgumentException("Invalid Base32 character: $c")
            bits.append(value.toString(2).padStart(5, '0'))
        }
        val bytes = ByteArray(bits.length / 8)
        for (i in bytes.indices) {
            bytes[i] = bits.substring(i * 8, i * 8 + 8).toInt(2).toByte()
        }
        return bytes
    }

    fun decodeBase64Url(encoded: String): String {
        return String(Base64.getUrlDecoder().decode(encoded))
    }

    fun encodeBase64Url(text: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
    }

    private fun copyWithPadding(
        src: ByteArray,
        dest: ByteArray,
        destOffset: Int,
        length: Int,
    ) {
        if (src.size <= length) {
            System.arraycopy(src, 0, dest, destOffset + length - src.size, src.size)
        } else {
            // Remove leading zero padding
            System.arraycopy(src, src.size - length, dest, destOffset, length)
        }
    }
}
