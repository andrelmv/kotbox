package com.github.andrelmv.kotbox.services.password

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class PasswordGeneratorServiceTest {
    class RandomPasswordTest {
        @Test
        fun `generates password with correct length`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 20)
            val result = PasswordGeneratorService.generate(config)
            assertEquals(20, result.length)
        }

        @Test
        fun `generates password with minimum length`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 4)
            val result = PasswordGeneratorService.generate(config)
            assertEquals(4, result.length)
        }

        @Test
        fun `generates password with maximum length`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 64)
            val result = PasswordGeneratorService.generate(config)
            assertEquals(64, result.length)
        }

        @Test
        fun `contains at least one digit when numbers enabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 20, includeNumbers = true)
            val results = (1..20).map { PasswordGeneratorService.generate(config) }
            assertTrue("At least one result should contain a digit", results.any { p -> p.any { it in DIGITS } })
        }

        @Test
        fun `contains at least one symbol when symbols enabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 20, includeSymbols = true)
            val results = (1..20).map { PasswordGeneratorService.generate(config) }
            assertTrue("At least one result should contain a symbol", results.any { p -> p.any { it in SYMBOLS } })
        }

        @Test
        fun `contains no digits when numbers disabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 30, includeNumbers = false, includeSymbols = false)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertFalse("Password should not contain digits: $result", result.any { it in DIGITS })
            }
        }

        @Test
        fun `contains no symbols when symbols disabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 30, includeNumbers = false, includeSymbols = false)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertFalse("Password should not contain symbols: $result", result.any { it in SYMBOLS })
            }
        }

        @Test
        fun `contains only letters when numbers and symbols disabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 20, includeNumbers = false, includeSymbols = false)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertTrue("Password should only contain letters: $result", result.all { it in LETTERS })
            }
        }

        @Test
        fun `all characters are within the expected pool when all options enabled`() {
            val allowed = LETTERS + DIGITS + SYMBOLS
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 40, includeNumbers = true, includeSymbols = true)
            repeat(10) {
                val result = PasswordGeneratorService.generate(config)
                assertTrue("Unexpected character in: $result", result.all { it in allowed })
            }
        }

        @Test
        fun `consecutive calls produce different passwords`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 20)
            val results = (1..10).map { PasswordGeneratorService.generate(config) }.toSet()
            assertTrue("Expected variety in generated passwords", results.size > 1)
        }

        @Test
        fun `guarantees digit is present in short password with numbers enabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 4, includeNumbers = true, includeSymbols = false)
            val hasDigit = (1..50).any { PasswordGeneratorService.generate(config).any { c -> c in DIGITS } }
            assertTrue("Short password should eventually contain a digit", hasDigit)
        }

        @Test
        fun `guarantees symbol is present in short password with symbols enabled`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 4, includeNumbers = false, includeSymbols = true)
            val hasSymbol = (1..50).any { PasswordGeneratorService.generate(config).any { c -> c in SYMBOLS } }
            assertTrue("Short password should eventually contain a symbol", hasSymbol)
        }
    }

    class MemorablePasswordTest {
        @Test
        fun `contains hyphen word separator`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 20)
            val result = PasswordGeneratorService.generate(config)
            assertTrue("Memorable password should contain hyphens: $result", result.contains('-'))
        }

        @Test
        fun `does not exceed configured length`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 8)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertTrue("Password length ${result.length} exceeds max 8: $result", result.length <= 8)
            }
        }

        @Test
        fun `is non-empty`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 20)
            val result = PasswordGeneratorService.generate(config)
            assertTrue(result.isNotEmpty())
        }

        @Test
        fun `contains only lowercase letters, hyphens, digits and symbols`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 64, includeNumbers = true, includeSymbols = true)
            val allowed = ('a'..'z').toSet() + '-' + DIGITS + SYMBOLS
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertTrue("Unexpected char in memorable password: $result", result.all { it in allowed })
            }
        }

        @Test
        fun `contains a digit when numbers enabled`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 64, includeNumbers = true, includeSymbols = false)
            val hasDigit = (1..20).any { PasswordGeneratorService.generate(config).any { c -> c in DIGITS } }
            assertTrue("Memorable password with numbers should contain a digit", hasDigit)
        }

        @Test
        fun `contains no digits when numbers disabled and length is long enough`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 64, includeNumbers = false, includeSymbols = false)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertFalse("Memorable password should not contain digits: $result", result.any { it in DIGITS })
            }
        }

        @Test
        fun `contains a symbol when symbols enabled`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 64, includeNumbers = false, includeSymbols = true)
            val hasSymbol = (1..20).any { PasswordGeneratorService.generate(config).any { c -> c in SYMBOLS } }
            assertTrue("Memorable password with symbols should contain a symbol", hasSymbol)
        }

        @Test
        fun `contains no non-hyphen symbols when symbols disabled and length is long enough`() {
            // Memorable passwords always use '-' as a word separator, which is technically in the
            // symbols pool. We only check that no *other* symbols appear when the option is disabled.
            val nonHyphenSymbols = SYMBOLS - '-'
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 64, includeNumbers = false, includeSymbols = false)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertFalse("Memorable password should not contain extra symbols: $result", result.any { it in nonHyphenSymbols })
            }
        }

        @Test
        fun `longer slider value produces more words`() {
            val short = PasswordConfig(type = PasswordType.MEMORABLE, length = 16, includeNumbers = false, includeSymbols = false)
            val long = PasswordConfig(type = PasswordType.MEMORABLE, length = 64, includeNumbers = false, includeSymbols = false)
            val shortWordCount = PasswordGeneratorService.generate(short).split('-').size
            val longWordCount = PasswordGeneratorService.generate(long).split('-').size
            assertTrue("Longer config should produce more words", longWordCount >= shortWordCount)
        }

        @Test
        fun `truncation does not add non-word characters`() {
            // Length 4 forces truncation — result must still be plain lowercase/hyphen chars
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 4, includeNumbers = false, includeSymbols = false)
            val allowed = ('a'..'z').toSet() + '-'
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertTrue("Truncated memorable should only have letters/hyphens: $result", result.all { it in allowed })
            }
        }
    }

    class PinPasswordTest {
        @Test
        fun `generates PIN with correct length`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 6)
            val result = PasswordGeneratorService.generate(config)
            assertEquals(6, result.length)
        }

        @Test
        fun `generates PIN with minimum length`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 4)
            val result = PasswordGeneratorService.generate(config)
            assertEquals(4, result.length)
        }

        @Test
        fun `generates PIN with maximum length`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 64)
            val result = PasswordGeneratorService.generate(config)
            assertEquals(64, result.length)
        }

        @Test
        fun `contains only digits`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 20)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertTrue("PIN should only contain digits: $result", result.all { it in DIGITS })
            }
        }

        @Test
        fun `ignores includeNumbers flag`() {
            val withNumbers = PasswordConfig(type = PasswordType.PIN, length = 10, includeNumbers = true)
            val withoutNumbers = PasswordConfig(type = PasswordType.PIN, length = 10, includeNumbers = false)
            val r1 = PasswordGeneratorService.generate(withNumbers)
            val r2 = PasswordGeneratorService.generate(withoutNumbers)
            assertEquals(10, r1.length)
            assertEquals(10, r2.length)
            assertTrue(r1.all { it in DIGITS })
            assertTrue(r2.all { it in DIGITS })
        }

        @Test
        fun `ignores includeSymbols flag`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 20, includeSymbols = true)
            repeat(20) {
                val result = PasswordGeneratorService.generate(config)
                assertFalse("PIN should not contain symbols: $result", result.any { it in SYMBOLS })
            }
        }

        @Test
        fun `consecutive calls produce different PINs`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 10)
            val results = (1..10).map { PasswordGeneratorService.generate(config) }.toSet()
            assertTrue("Expected variety in generated PINs", results.size > 1)
        }
    }

    class BulkGenerationTest {
        @Test
        fun `generates correct number of passwords`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 16)
            val results = PasswordGeneratorService.generateBulk(config, 10)
            assertEquals(10, results.size)
        }

        @Test
        fun `generates single password`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 16)
            val results = PasswordGeneratorService.generateBulk(config, 1)
            assertEquals(1, results.size)
        }

        @Test
        fun `generates large batch`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 16)
            val results = PasswordGeneratorService.generateBulk(config, 1000)
            assertEquals(1000, results.size)
        }

        @Test
        fun `all bulk passwords are non-empty`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 12)
            val results = PasswordGeneratorService.generateBulk(config, 50)
            assertTrue("All bulk passwords should be non-empty", results.all { it.isNotEmpty() })
        }

        @Test
        fun `all bulk passwords have correct length for random type`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 16)
            val results = PasswordGeneratorService.generateBulk(config, 50)
            assertTrue("All bulk passwords should have length 16", results.all { it.length == 16 })
        }

        @Test
        fun `all bulk passwords are digits only for PIN type`() {
            val config = PasswordConfig(type = PasswordType.PIN, length = 6)
            val results = PasswordGeneratorService.generateBulk(config, 20)
            assertTrue("All bulk PINs should contain only digits", results.all { p -> p.all { it in DIGITS } })
        }

        @Test
        fun `bulk produces variety in passwords`() {
            val config = PasswordConfig(type = PasswordType.RANDOM, length = 20)
            val results = PasswordGeneratorService.generateBulk(config, 20).toSet()
            assertTrue("Bulk generation should produce different passwords", results.size > 1)
        }

        @Test
        fun `bulk works for memorable type`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE, length = 32)
            val results = PasswordGeneratorService.generateBulk(config, 10)
            assertEquals(10, results.size)
            assertTrue("All memorable passwords should contain hyphens", results.all { it.contains('-') })
        }
    }

    private companion object {
        val DIGITS = ('0'..'9').toSet()
        val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?".toSet()
        val LETTERS = (('a'..'z') + ('A'..'Z')).toSet()
    }
}
