package com.github.andrelmv.kotbox.services.password

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class PasswordConfigTest {
    class PasswordTypeTest {
        @Test
        fun `RANDOM has correct label`() {
            assertEquals("Random", PasswordType.RANDOM.label)
        }

        @Test
        fun `MEMORABLE has correct label`() {
            assertEquals("Memorable", PasswordType.MEMORABLE.label)
        }

        @Test
        fun `PIN has correct label`() {
            assertEquals("PIN", PasswordType.PIN.label)
        }

        @Test
        fun `enum has three values`() {
            assertEquals(3, PasswordType.entries.size)
        }
    }

    class PasswordConfigDefaultsTest {
        @Test
        fun `default type is RANDOM`() {
            assertEquals(PasswordType.RANDOM, PasswordConfig().type)
        }

        @Test
        fun `default length is 20`() {
            assertEquals(20, PasswordConfig().length)
        }

        @Test
        fun `default includeNumbers is true`() {
            assertTrue(PasswordConfig().includeNumbers)
        }

        @Test
        fun `default includeSymbols is true`() {
            assertTrue(PasswordConfig().includeSymbols)
        }
    }

    class PasswordConfigCustomValuesTest {
        @Test
        fun `can set MEMORABLE type`() {
            val config = PasswordConfig(type = PasswordType.MEMORABLE)
            assertEquals(PasswordType.MEMORABLE, config.type)
        }

        @Test
        fun `can set PIN type`() {
            val config = PasswordConfig(type = PasswordType.PIN)
            assertEquals(PasswordType.PIN, config.type)
        }

        @Test
        fun `can set custom length`() {
            val config = PasswordConfig(length = 32)
            assertEquals(32, config.length)
        }

        @Test
        fun `can disable numbers`() {
            val config = PasswordConfig(includeNumbers = false)
            assertFalse(config.includeNumbers)
        }

        @Test
        fun `can disable symbols`() {
            val config = PasswordConfig(includeSymbols = false)
            assertFalse(config.includeSymbols)
        }
    }

    class PasswordConfigEqualityTest {
        @Test
        fun `two configs with same values are equal`() {
            val a = PasswordConfig(PasswordType.RANDOM, 20, includeNumbers = true, includeSymbols = false)
            val b = PasswordConfig(PasswordType.RANDOM, 20, includeNumbers = true, includeSymbols = false)
            assertEquals(a, b)
        }

        @Test
        fun `copy preserves all values`() {
            val original = PasswordConfig(PasswordType.MEMORABLE, 16, includeNumbers = false, includeSymbols = true)
            val copy = original.copy()
            assertEquals(original, copy)
        }

        @Test
        fun `copy can override individual fields`() {
            val original = PasswordConfig(PasswordType.RANDOM, 20, includeNumbers = true, includeSymbols = true)
            val modified = original.copy(length = 8, includeSymbols = false)
            assertEquals(8, modified.length)
            assertFalse(modified.includeSymbols)
            assertEquals(PasswordType.RANDOM, modified.type)
            assertTrue(modified.includeNumbers)
        }
    }
}
