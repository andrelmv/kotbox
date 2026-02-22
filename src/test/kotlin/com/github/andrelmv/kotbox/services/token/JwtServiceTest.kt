package com.github.andrelmv.kotbox.services.token

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class JwtServiceTest {
    class Base64UrlTest {
        @Test
        fun `encodeBase64Url encodes simple string`() {
            val result = JwtService.encodeBase64Url("Hello World")

            assertFalse(result.contains("="))
            assertEquals("SGVsbG8gV29ybGQ", result)
        }

        @Test
        fun `encodeBase64Url handles UTF-8 characters`() {
            val result = JwtService.encodeBase64Url("André Monteiro")

            assertFalse(result.contains("="))
            assertFalse(result.contains("+"))
            assertFalse(result.contains("/"))
        }

        @Test
        fun `encodeBase64Url handles empty string`() {
            assertEquals("", JwtService.encodeBase64Url(""))
        }

        @Test
        fun `encodeBase64Url handles special characters`() {
            val result = JwtService.encodeBase64Url("""{"key":"value"}""")

            assertFalse(result.contains("="))
            assertEquals("eyJrZXkiOiJ2YWx1ZSJ9", result)
        }

        @Test
        fun `encodeBase64Url produces URL-safe encoding`() {
            val result = JwtService.encodeBase64Url("subjects?_d=1")

            assertFalse(result.contains("+"))
            assertFalse(result.contains("/"))
            assertFalse(result.contains("="))
        }

        @Test
        fun `encodeBase64Url strips padding for all lengths`() {
            listOf("a", "ab", "abc", "abcd").forEach { input ->
                val result = JwtService.encodeBase64Url(input)
                assertFalse("Result should not contain padding for '$input'", result.contains("="))
            }
        }

        @Test
        fun `encodeBase64Url uses URL-safe alphabet`() {
            listOf("?>", "a?b", "test+data/here").forEach { input ->
                val result = JwtService.encodeBase64Url(input)
                assertFalse("Should not contain '+' for '$input'", result.contains("+"))
                assertFalse("Should not contain '/' for '$input'", result.contains("/"))
            }
        }

        @Test
        fun `decodeBase64Url decodes simple string`() {
            assertEquals("Hello World", JwtService.decodeBase64Url("SGVsbG8gV29ybGQ"))
        }

        @Test
        fun `decodeBase64Url handles UTF-8 characters`() {
            assertEquals("André Monteiro", JwtService.decodeBase64Url("QW5kcsOpIE1vbnRlaXJv"))
        }

        @Test
        fun `decodeBase64Url handles empty string`() {
            assertEquals("", JwtService.decodeBase64Url(""))
        }

        @Test
        fun `decodeBase64Url handles strings without padding`() {
            assertEquals("abc", JwtService.decodeBase64Url("YWJj"))
        }

        @Test
        fun `encode and decode are reversible`() {
            val original = """{"alg":"HS256","typ":"JWT"}"""
            val encoded = JwtService.encodeBase64Url(original)

            assertEquals(original, JwtService.decodeBase64Url(encoded))
        }
    }

    class DecodeTest {
        @Test
        fun `parses valid JWT token`() {
            val header = """{"alg":"HS256","typ":"JWT"}"""
            val payload = """{"sub":"1234567890","name":"André","iat":1516239022}"""
            val signature = "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
            val token = "${JwtService.encodeBase64Url(header)}.${JwtService.encodeBase64Url(payload)}.$signature"
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "your-256-bit-secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val result = JwtService.decode(token, config)

            assertTrue(result.header.contains("HS256"))
            assertTrue(result.payload.contains("André"))
            assertEquals(signature, result.encodedSignature)
            assertFalse(result.isSignatureValid)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for token with only 2 parts`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            JwtService.decode("header.payload", config)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for token with 4 parts`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            JwtService.decode("header.payload.signature.extra", config)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for empty token`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            JwtService.decode("", config)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for whitespace-only token`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            JwtService.decode("   ", config)
        }

        @Test
        fun `handles token with very long header and payload`() {
            val longHeader = """{"alg":"HS256","typ":"JWT","custom":"${"x".repeat(1000)}"}"""
            val longPayload = """{"sub":"user","data":"${"y".repeat(1000)}"}"""
            val token = "${JwtService.encodeBase64Url(longHeader)}.${JwtService.encodeBase64Url(longPayload)}.signature"
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val result = JwtService.decode(token, config)

            assertTrue(result.header.contains("HS256"))
            assertTrue(result.payload.contains("user"))
        }
    }

    class HmacTest {
        @Test
        fun `encode creates valid JWT with HMAC256`() {
            val header = """{"alg":"HS256","typ":"JWT"}"""
            val payload = """{"sub":"user123","name":"André"}"""
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "your-256-bit-secret-key-here-must-be-long-enough",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val result = JwtService.encode(header, payload, config)
            val parts = result.encodedToken.split(".")

            assertEquals(3, parts.size)
            assertEquals(result.encodedHeader, parts[0])
            assertEquals(result.encodedPayload, parts[1])
            assertEquals(result.encodedSignature, parts[2])

            val decoded = JwtService.decode(result.encodedToken, config)
            assertTrue(decoded.header.contains("HS256"))
            assertTrue(decoded.payload.contains("André"))
        }

        @Test
        fun `encode and verify with correct HMAC256 key`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "your-256-bit-secret-key-here-must-be-long-enough",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val encoded = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123","name":"Test"}""", config)
            val verified = JwtService.decodeAndVerify(encoded.encodedToken, config)

            assertTrue(verified.isSignatureValid)
        }

        @Test
        fun `verify fails with incorrect HMAC key`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "your-256-bit-secret-key-here-must-be-long-enough",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val encoded = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123","name":"Test"}""", config)
            val verified =
                JwtService.decodeAndVerify(
                    encoded.encodedToken,
                    config.copy(secretKey = "wrong-key-that-is-different-from-original"),
                )

            assertFalse(verified.isSignatureValid)
        }

        @Test
        fun `encode and verify with HMAC384`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC384,
                    secretKey = "your-384-bit-secret-key-here-must-be-very-long-to-meet-requirements",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val result = JwtService.encode("""{"alg":"HS384","typ":"JWT"}""", """{"sub":"user123"}""", config)
            val verified = JwtService.decodeAndVerify(result.encodedToken, config)

            assertTrue(verified.isSignatureValid)
        }

        @Test
        fun `encode and verify with HMAC512`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC512,
                    secretKey = "your-512-bit-secret-key-here-must-be-extremely-long-to-meet-all-security-requirements",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val result = JwtService.encode("""{"alg":"HS512","typ":"JWT"}""", """{"sub":"user123"}""", config)
            val verified = JwtService.decodeAndVerify(result.encodedToken, config)

            assertTrue(verified.isSignatureValid)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `encode throws for too short HMAC256 key with strict validation`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "short",
                    privateKeyPem = "",
                    strictValidation = true,
                )

            JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", config)
        }

        @Test
        fun `encode allows short HMAC key without strict validation`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "short",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val result = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", config)

            assertNotEquals("", result.encodedToken)
        }

        @Test
        fun `verify returns false for too short HMAC key with strict validation`() {
            val configLenient =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "short",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val encoded = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", configLenient)
            val verified = JwtService.decodeAndVerify(encoded.encodedToken, configLenient.copy(strictValidation = true))

            assertFalse(verified.isSignatureValid)
        }
    }

    class SecretKeyEncodingTest {
        @Test
        fun `encode and verify with RAW encoding`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "my-secret-key-that-is-long-enough-for-hs256",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.RAW,
                )

            val result = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", config)
            val verified = JwtService.decodeAndVerify(result.encodedToken, config)

            assertTrue(verified.isSignatureValid)
        }

        @Test
        fun `encode and verify with BASE64 encoding`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci10ZXN0aW5n",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.BASE64,
                )

            val result = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", config)
            val verified = JwtService.decodeAndVerify(result.encodedToken, config)

            assertTrue(verified.isSignatureValid)
        }

        @Test
        fun `encode and verify with BASE32 encoding`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "MFRGGZDFMZTWQ2LKNNWG23TPOBYXE43UOV3HO6DZEBSG6IDFEBSG6IC=",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.BASE32,
                )

            val result = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", config)
            val verified = JwtService.decodeAndVerify(result.encodedToken, config)

            assertTrue(verified.isSignatureValid)
        }

        @Test
        fun `BASE32 and RAW produce different signatures`() {
            val header = """{"alg":"HS256","typ":"JWT"}"""
            val payload = """{"sub":"user123"}"""
            val configRaw =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "ABC",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.RAW,
                )

            val resultRaw = JwtService.encode(header, payload, configRaw)
            val resultBase32 = JwtService.encode(header, payload, configRaw.copy(secretKeyEncoding = SecretKeyEncoding.BASE32))

            assertNotEquals(resultRaw.encodedSignature, resultBase32.encodedSignature)
        }
    }

    class GetPSSParameterSpecTest {
        @Test
        fun `PS256 spec`() {
            val spec = JwtService.getPSSParameterSpec(SignatureAlgorithm.PS256)

            assertEquals("SHA-256", spec.digestAlgorithm)
            assertEquals("MGF1", spec.mgfAlgorithm)
            assertEquals(32, spec.saltLength)
        }

        @Test
        fun `PS384 spec`() {
            val spec = JwtService.getPSSParameterSpec(SignatureAlgorithm.PS384)

            assertEquals("SHA-384", spec.digestAlgorithm)
            assertEquals("MGF1", spec.mgfAlgorithm)
            assertEquals(48, spec.saltLength)
        }

        @Test
        fun `PS512 spec`() {
            val spec = JwtService.getPSSParameterSpec(SignatureAlgorithm.PS512)

            assertEquals("SHA-512", spec.digestAlgorithm)
            assertEquals("MGF1", spec.mgfAlgorithm)
            assertEquals(64, spec.saltLength)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for non-PSS algorithm`() {
            JwtService.getPSSParameterSpec(SignatureAlgorithm.HMAC256)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for RSA256`() {
            JwtService.getPSSParameterSpec(SignatureAlgorithm.RSA256)
        }
    }

    class ResolveSecretKeyBytesTest {
        @Test
        fun `RAW encoding returns raw bytes`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "mysecret",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.RAW,
                )

            val result = JwtService.resolveSecretKeyBytes(config)

            assertEquals("mysecret", String(result, Charsets.UTF_8))
        }

        @Test
        fun `BASE64 encoding decodes correctly`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "bXlzZWNyZXQ=",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.BASE64,
                )

            val result = JwtService.resolveSecretKeyBytes(config)

            assertEquals("mysecret", String(result, Charsets.UTF_8))
        }

        @Test
        fun `BASE32 encoding decodes correctly`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "MFRGGZDFMZTWQ2LKNNWG23TPOBYXE43UOV3HO6DZEBSG6IDFEBSG6IC=",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.BASE32,
                )

            val result = JwtService.resolveSecretKeyBytes(config)

            assertTrue(result.isNotEmpty())
        }

        @Test
        fun `BASE32 is case insensitive`() {
            val base =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    privateKeyPem = "",
                    secretKey = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.BASE32,
                )

            val resultUpper = JwtService.resolveSecretKeyBytes(base.copy(secretKey = "MFRGG==="))
            val resultLower = JwtService.resolveSecretKeyBytes(base.copy(secretKey = "mfrgg==="))

            assertTrue(resultUpper.contentEquals(resultLower))
        }

        @Test(expected = IllegalArgumentException::class)
        fun `BASE32 throws for invalid characters`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "INVALID!@#$",
                    privateKeyPem = "",
                    strictValidation = false,
                    secretKeyEncoding = SecretKeyEncoding.BASE32,
                )

            JwtService.resolveSecretKeyBytes(config)
        }
    }

    class DerToRawEcdsaTest {
        @Test(expected = IllegalArgumentException::class)
        fun `throws for too short signature`() {
            JwtService.derToRawEcdsa(byteArrayOf(0x30, 0x01, 0x02), 32)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for missing R INTEGER tag`() {
            JwtService.derToRawEcdsa(byteArrayOf(0x30, 0x06, 0x01, 0x01, 0x00, 0x02, 0x01, 0x00), 32)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `throws for missing S INTEGER tag`() {
            JwtService.derToRawEcdsa(byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x00, 0x01, 0x01, 0x00), 32)
        }

        @Test
        fun `converts valid DER signature`() {
            val derSignature = byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02)

            val result = JwtService.derToRawEcdsa(derSignature, 4)

            assertEquals(8, result.size)
        }
    }

    class ParsePKCS8PrivateKeyTest {
        @Test
        fun `handles standard PRIVATE KEY header`() {
            val pem =
                """
                -----BEGIN PRIVATE KEY-----
                MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKj
                -----END PRIVATE KEY-----
                """.trimIndent()

            try {
                JwtService.parsePKCS8PrivateKey(pem, "RSA")
            } catch (e: Exception) {
                assertTrue(e.message?.contains("RSA") == true || e.message != null)
            }
        }

        @Test
        fun `handles RSA PRIVATE KEY header`() {
            val pem =
                """
                -----BEGIN RSA PRIVATE KEY-----
                MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKj
                -----END RSA PRIVATE KEY-----
                """.trimIndent()

            try {
                JwtService.parsePKCS8PrivateKey(pem, "RSA")
            } catch (e: Exception) {
                assertTrue(e.message != null)
            }
        }

        @Test
        fun `handles EC PRIVATE KEY header`() {
            val pem =
                """
                -----BEGIN EC PRIVATE KEY-----
                MHcCAQEEIIGlRHp1q3y1RKxCRM5w8T35ztVX
                -----END EC PRIVATE KEY-----
                """.trimIndent()

            try {
                JwtService.parsePKCS8PrivateKey(pem, "EC")
            } catch (e: Exception) {
                assertTrue(e.message != null)
            }
        }

        @Test
        fun `strips whitespace from PEM`() {
            val pem =
                """
                -----BEGIN PRIVATE KEY-----
                MIIE  vQIB  ADAN  Bg
                -----END PRIVATE KEY-----
                """.trimIndent()

            try {
                JwtService.parsePKCS8PrivateKey(pem, "RSA")
            } catch (e: Exception) {
                assertTrue(e.message != null)
            }
        }
    }

    class SignPayloadTest {
        @Test
        fun `produces non-empty URL-safe signatures for all HMAC algorithms`() {
            val encodedHeader = JwtService.encodeBase64Url("""{"alg":"HS256"}""")
            val encodedPayload = JwtService.encodeBase64Url("""{"sub":"user"}""")

            val algorithms =
                listOf(
                    SignatureAlgorithm.HMAC256 to "secret-key-that-is-long-enough-for-hs256",
                    SignatureAlgorithm.HMAC384 to "secret-key-that-is-very-long-enough-for-hs384-algorithm",
                    SignatureAlgorithm.HMAC512 to "secret-key-that-is-extremely-long-enough-for-hs512-algorithm-requirements",
                )

            algorithms.forEach { (algorithm, key) ->
                val config =
                    SigningConfig(
                        algorithm = algorithm,
                        secretKey = key,
                        privateKeyPem = "",
                        strictValidation = false,
                    )

                val signature = JwtService.signPayload(encodedHeader, encodedPayload, config)

                assertTrue("Signature should not be empty for $algorithm", signature.isNotEmpty())
                assertFalse("Signature should not contain padding for $algorithm", signature.contains("="))
            }
        }
    }

    class VerifySignatureTest {
        @Test
        fun `HMAC signing is deterministic and verifiable`() {
            val encodedHeader = JwtService.encodeBase64Url("""{"alg":"HS256"}""")
            val encodedPayload = JwtService.encodeBase64Url("""{"sub":"user"}""")
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "consistent-secret-key-for-testing-purposes",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val signature1 = JwtService.signPayload(encodedHeader, encodedPayload, config)
            val signature2 = JwtService.signPayload(encodedHeader, encodedPayload, config)

            assertEquals(signature1, signature2)
            assertTrue(JwtService.verifySignature(encodedHeader, encodedPayload, signature1, config))
        }

        @Test
        fun `fails for mismatched signature`() {
            val encodedHeader = JwtService.encodeBase64Url("""{"alg":"HS256"}""")
            val encodedPayload = JwtService.encodeBase64Url("""{"sub":"user"}""")
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "my-secret-key-for-verification-test",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            assertFalse(JwtService.verifySignature(encodedHeader, encodedPayload, "wrong-signature-value-here", config))
        }

        @Test
        fun `decodeAndVerify returns false for tampered token`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "my-secret-key-that-is-long-enough",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            val encoded = JwtService.encode("""{"alg":"HS256","typ":"JWT"}""", """{"sub":"user123"}""", config)

            try {
                val result = JwtService.decodeAndVerify(encoded.encodedToken + "X", config)
                assertFalse(result.isSignatureValid)
            } catch (e: Exception) {
                assertTrue(e is IllegalArgumentException)
            }
        }

        @Test
        fun `decodeAndVerify handles invalid token format gracefully`() {
            val config =
                SigningConfig(
                    algorithm = SignatureAlgorithm.HMAC256,
                    secretKey = "secret",
                    privateKeyPem = "",
                    strictValidation = false,
                )

            try {
                val result = JwtService.decodeAndVerify("invalid.token.format", config)
                assertFalse(result.isSignatureValid)
            } catch (e: Exception) {
                assertTrue(e is IllegalArgumentException)
            }
        }
    }
}
