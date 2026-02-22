package com.github.andrelmv.kotbox.services.token

import org.junit.Assert.assertEquals
import org.junit.Test

class JwtServiceRsaTest {
    @Test
    fun `test getPSSParameterSpec should return correct spec for PS256`() {
        val spec = JwtService.getPSSParameterSpec(SignatureAlgorithm.PS256)

        assertEquals("SHA-256", spec.digestAlgorithm)
        assertEquals("MGF1", spec.mgfAlgorithm)
        assertEquals(32, spec.saltLength)
    }

    @Test
    fun `test getPSSParameterSpec should return correct spec for PS384`() {
        val spec = JwtService.getPSSParameterSpec(SignatureAlgorithm.PS384)

        assertEquals("SHA-384", spec.digestAlgorithm)
        assertEquals("MGF1", spec.mgfAlgorithm)
        assertEquals(48, spec.saltLength)
    }

    @Test
    fun `test getPSSParameterSpec should return correct spec for PS512`() {
        val spec = JwtService.getPSSParameterSpec(SignatureAlgorithm.PS512)

        assertEquals("SHA-512", spec.digestAlgorithm)
        assertEquals("MGF1", spec.mgfAlgorithm)
        assertEquals(64, spec.saltLength)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test getPSSParameterSpec should throw exception for non-PSS algorithm`() {
        JwtService.getPSSParameterSpec(SignatureAlgorithm.RSA256)
    }
}
