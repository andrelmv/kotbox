package com.github.andrelmv.kotbox.services.token

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class JwtModelsTest {
    class AlgorithmKindTest {
        @Test
        fun `HMAC has correct label`() {
            assertEquals("Secret key:", AlgorithmKind.HMAC.label)
        }

        @Test
        fun `RSA has correct label`() {
            assertEquals("Private key:", AlgorithmKind.RSA.label)
        }

        @Test
        fun `ECDSA has correct label`() {
            assertEquals("Private key:", AlgorithmKind.ECDSA.label)
        }

        @Test
        fun `RSA_PSS has correct label`() {
            assertEquals("Private key:", AlgorithmKind.RSA_PSS.label)
        }

        @Test
        fun `EDDSA has correct label`() {
            assertEquals("Private key:", AlgorithmKind.EDDSA.label)
        }

        @Test
        fun `enum has correct values count`() {
            assertEquals(5, AlgorithmKind.entries.size)
        }
    }

    class SignatureAlgorithmTest {
        @Test
        fun `HMAC256 properties`() {
            val algorithm = SignatureAlgorithm.HMAC256
            assertEquals("HMAC256 (HS256)", algorithm.displayName)
            assertEquals("HS256", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.HMAC, algorithm.kind)
            assertEquals("HmacSHA256", algorithm.jdkAlgorithm)
            assertEquals(32, algorithm.minKeyBytes)
            assertEquals(0, algorithm.ecComponentSize)
        }

        @Test
        fun `HMAC384 properties`() {
            val algorithm = SignatureAlgorithm.HMAC384
            assertEquals("HMAC384 (HS384)", algorithm.displayName)
            assertEquals("HS384", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.HMAC, algorithm.kind)
            assertEquals("HmacSHA384", algorithm.jdkAlgorithm)
            assertEquals(48, algorithm.minKeyBytes)
        }

        @Test
        fun `HMAC512 properties`() {
            val algorithm = SignatureAlgorithm.HMAC512
            assertEquals("HMAC512 (HS512)", algorithm.displayName)
            assertEquals("HS512", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.HMAC, algorithm.kind)
            assertEquals("HmacSHA512", algorithm.jdkAlgorithm)
            assertEquals(64, algorithm.minKeyBytes)
        }

        @Test
        fun `RSA256 properties`() {
            val algorithm = SignatureAlgorithm.RSA256
            assertEquals("RSA256 (RS256)", algorithm.displayName)
            assertEquals("RS256", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.RSA, algorithm.kind)
            assertEquals("SHA256withRSA", algorithm.jdkAlgorithm)
            assertEquals(0, algorithm.minKeyBytes)
        }

        @Test
        fun `RSA384 properties`() {
            val algorithm = SignatureAlgorithm.RSA384
            assertEquals("RSA384 (RS384)", algorithm.displayName)
            assertEquals("RS384", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.RSA, algorithm.kind)
            assertEquals("SHA384withRSA", algorithm.jdkAlgorithm)
        }

        @Test
        fun `RSA512 properties`() {
            val algorithm = SignatureAlgorithm.RSA512
            assertEquals("RSA512 (RS512)", algorithm.displayName)
            assertEquals("RS512", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.RSA, algorithm.kind)
            assertEquals("SHA512withRSA", algorithm.jdkAlgorithm)
        }

        @Test
        fun `ECDSA256 properties`() {
            val algorithm = SignatureAlgorithm.ECDSA256
            assertEquals("ECDSA256 (ES256)", algorithm.displayName)
            assertEquals("ES256", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.ECDSA, algorithm.kind)
            assertEquals("SHA256withECDSA", algorithm.jdkAlgorithm)
            assertEquals(32, algorithm.ecComponentSize)
        }

        @Test
        fun `ECDSA384 properties`() {
            val algorithm = SignatureAlgorithm.ECDSA384
            assertEquals("ECDSA384 (ES384)", algorithm.displayName)
            assertEquals("ES384", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.ECDSA, algorithm.kind)
            assertEquals("SHA384withECDSA", algorithm.jdkAlgorithm)
            assertEquals(48, algorithm.ecComponentSize)
        }

        @Test
        fun `ECDSA512 properties`() {
            val algorithm = SignatureAlgorithm.ECDSA512
            assertEquals("ECDSA512 (ES512)", algorithm.displayName)
            assertEquals("ES512", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.ECDSA, algorithm.kind)
            assertEquals("SHA512withECDSA", algorithm.jdkAlgorithm)
            assertEquals(66, algorithm.ecComponentSize)
        }

        @Test
        fun `PS256 properties`() {
            val algorithm = SignatureAlgorithm.PS256
            assertEquals("PS256", algorithm.displayName)
            assertEquals("PS256", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.RSA_PSS, algorithm.kind)
            assertEquals("RSASSA-PSS", algorithm.jdkAlgorithm)
        }

        @Test
        fun `PS384 properties`() {
            val algorithm = SignatureAlgorithm.PS384
            assertEquals("PS384", algorithm.displayName)
            assertEquals("PS384", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.RSA_PSS, algorithm.kind)
            assertEquals("RSASSA-PSS", algorithm.jdkAlgorithm)
        }

        @Test
        fun `PS512 properties`() {
            val algorithm = SignatureAlgorithm.PS512
            assertEquals("PS512", algorithm.displayName)
            assertEquals("PS512", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.RSA_PSS, algorithm.kind)
            assertEquals("RSASSA-PSS", algorithm.jdkAlgorithm)
        }

        @Test
        fun `EDDSA properties`() {
            val algorithm = SignatureAlgorithm.EDDSA
            assertEquals("EdDSA", algorithm.displayName)
            assertEquals("EdDSA", algorithm.jwtHeaderValue)
            assertEquals(AlgorithmKind.EDDSA, algorithm.kind)
            assertEquals("Ed25519", algorithm.jdkAlgorithm)
        }

        @Test
        fun `toString returns displayName`() {
            assertEquals("HMAC256 (HS256)", SignatureAlgorithm.HMAC256.toString())
            assertEquals("RSA256 (RS256)", SignatureAlgorithm.RSA256.toString())
            assertEquals("ECDSA256 (ES256)", SignatureAlgorithm.ECDSA256.toString())
            assertEquals("PS256", SignatureAlgorithm.PS256.toString())
            assertEquals("EdDSA", SignatureAlgorithm.EDDSA.toString())
        }

        @Test
        fun `enum has correct values count`() {
            assertEquals(13, SignatureAlgorithm.entries.size)
        }
    }

    class DecodeResultTest {
        @Test
        fun `creation with default signature validation`() {
            val result =
                DecodeResult(
                    header = """{"alg":"HS256"}""",
                    payload = """{"sub":"user123"}""",
                    encodedSignature = "signature123",
                )

            assertEquals("""{"alg":"HS256"}""", result.header)
            assertEquals("""{"sub":"user123"}""", result.payload)
            assertEquals("signature123", result.encodedSignature)
            assertFalse(result.isSignatureValid)
        }

        @Test
        fun `creation with valid signature`() {
            val result =
                DecodeResult(
                    header = """{"alg":"HS256"}""",
                    payload = """{"sub":"user123"}""",
                    encodedSignature = "signature123",
                    isSignatureValid = true,
                )

            assertTrue(result.isSignatureValid)
        }

        @Test
        fun `copy function`() {
            val original =
                DecodeResult(
                    header = """{"alg":"HS256"}""",
                    payload = """{"sub":"user123"}""",
                    encodedSignature = "signature123",
                    isSignatureValid = false,
                )

            val modified = original.copy(isSignatureValid = true)

            assertFalse(original.isSignatureValid)
            assertTrue(modified.isSignatureValid)
            assertEquals(original.header, modified.header)
            assertEquals(original.payload, modified.payload)
        }

        @Test
        fun equality() {
            val result1 =
                DecodeResult(
                    header = """{"alg":"HS256"}""",
                    payload = """{"sub":"user123"}""",
                    encodedSignature = "signature123",
                )

            val result2 =
                DecodeResult(
                    header = """{"alg":"HS256"}""",
                    payload = """{"sub":"user123"}""",
                    encodedSignature = "signature123",
                )

            assertEquals(result1, result2)
        }
    }

    class EncodeResultTest {
        @Test
        fun creation() {
            val result =
                EncodeResult(
                    encodedToken = "header.payload.signature",
                    encodedHeader = "header",
                    encodedPayload = "payload",
                    encodedSignature = "signature",
                )

            assertEquals("header.payload.signature", result.encodedToken)
            assertEquals("header", result.encodedHeader)
            assertEquals("payload", result.encodedPayload)
            assertEquals("signature", result.encodedSignature)
        }

        @Test
        fun `copy function`() {
            val original =
                EncodeResult(
                    encodedToken = "header.payload.signature",
                    encodedHeader = "header",
                    encodedPayload = "payload",
                    encodedSignature = "signature",
                )

            val modified = original.copy(encodedSignature = "newSignature")

            assertEquals("signature", original.encodedSignature)
            assertEquals("newSignature", modified.encodedSignature)
        }

        @Test
        fun equality() {
            val result1 =
                EncodeResult(
                    encodedToken = "header.payload.signature",
                    encodedHeader = "header",
                    encodedPayload = "payload",
                    encodedSignature = "signature",
                )

            val result2 =
                EncodeResult(
                    encodedToken = "header.payload.signature",
                    encodedHeader = "header",
                    encodedPayload = "payload",
                    encodedSignature = "signature",
                )

            assertEquals(result1, result2)
        }
    }

    class SecretKeyEncodingTest {
        @Test
        fun `RAW display name`() {
            assertEquals("Raw", SecretKeyEncoding.RAW.displayName)
        }

        @Test
        fun `BASE32 display name`() {
            assertEquals("Base32 Encoded", SecretKeyEncoding.BASE32.displayName)
        }

        @Test
        fun `BASE64 display name`() {
            assertEquals("Base64 Encoded", SecretKeyEncoding.BASE64.displayName)
        }

        @Test
        fun `enum has correct values count`() {
            assertEquals(3, SecretKeyEncoding.entries.size)
        }
    }

    class SigningConfigTest {
        @Test
        fun `creation with HMAC algorithm`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "my-secret-key",
                    privateKeyPem = "",
                    strictValidation = true,
                )

            assertEquals(SignatureAlgorithm.HMAC256, config.algorithm)
            assertEquals("my-secret-key", config.secretKey)
            assertEquals("", config.privateKeyPem)
            assertTrue(config.strictValidation)
            assertEquals(SecretKeyEncoding.RAW, config.secretKeyEncoding)
        }

        @Test
        fun `creation with RSA algorithm`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.RSA256,
                    secretKey = "",
                    privateKeyPem = "-----BEGIN PRIVATE KEY-----",
                    strictValidation = false,
                )

            assertEquals(SignatureAlgorithm.RSA256, config.algorithm)
            assertEquals("-----BEGIN PRIVATE KEY-----", config.privateKeyPem)
            assertFalse(config.strictValidation)
        }

        @Test
        fun `creation with BASE64 encoding`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "base64encodedkey",
                    privateKeyPem = "",
                    strictValidation = true,
                    secretKeyEncoding = SecretKeyEncoding.BASE64,
                )

            assertEquals(SecretKeyEncoding.BASE64, config.secretKeyEncoding)
        }

        @Test
        fun `copy function`() {
            val original =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val modified = original.copy(strictValidation = true)

            assertFalse(original.strictValidation)
            assertTrue(modified.strictValidation)
            assertEquals(original.algorithm, modified.algorithm)
        }

        @Test
        fun equality() {
            val config1 =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = true,
                )

            val config2 =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = true,
                )

            assertEquals(config1, config2)
        }

        @Test
        fun `supports all SecretKeyEncoding types`() {
            val configRaw =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.RAW,
                )

            val configBase32 = configRaw.copy(secretKeyEncoding = SecretKeyEncoding.BASE32)
            val configBase64 = configRaw.copy(secretKeyEncoding = SecretKeyEncoding.BASE64)

            assertEquals(SecretKeyEncoding.RAW, configRaw.secretKeyEncoding)
            assertEquals(SecretKeyEncoding.BASE32, configBase32.secretKeyEncoding)
            assertEquals(SecretKeyEncoding.BASE64, configBase64.secretKeyEncoding)
        }
    }
}
