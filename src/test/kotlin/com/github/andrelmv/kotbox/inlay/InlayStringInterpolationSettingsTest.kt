package com.github.andrelmv.kotbox.inlay

import com.github.andrelmv.kotbox.inlay.config.InlayStringInterpolationSettings
import com.github.andrelmv.kotbox.inlay.config.InlayStringInterpolationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InlayStringInterpolationSettingsTest {
    @Test
    fun `test InlayStringInterpolationState default values`() {
        val state = InlayStringInterpolationState()

        assertTrue(state.withStringInterpolationHint)
        assertTrue(state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState can change withStringInterpolationHint`() {
        val state = InlayStringInterpolationState()

        state.withStringInterpolationHint = false

        assertFalse(state.withStringInterpolationHint)
        assertTrue(state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState can change withStringConstantHint`() {
        val state = InlayStringInterpolationState()

        state.withStringConstantHint = false

        assertTrue(state.withStringInterpolationHint)
        assertFalse(state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState can change both properties`() {
        val state = InlayStringInterpolationState()

        state.withStringInterpolationHint = false
        state.withStringConstantHint = false

        assertFalse(state.withStringInterpolationHint)
        assertFalse(state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState can set both properties to true`() {
        val state = InlayStringInterpolationState()

        state.withStringInterpolationHint = false
        state.withStringConstantHint = false

        state.withStringInterpolationHint = true
        state.withStringConstantHint = true

        assertTrue(state.withStringInterpolationHint)
        assertTrue(state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationSettings has default state`() {
        val settings = InlayStringInterpolationSettings()

        assertNotNull(settings.state)
        assertTrue(settings.state.withStringInterpolationHint)
        assertTrue(settings.state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationSettings state can be modified`() {
        val settings = InlayStringInterpolationSettings()

        settings.state.withStringInterpolationHint = false

        assertFalse(settings.state.withStringInterpolationHint)
    }

    @Test
    fun `test InlayStringInterpolationSettings loadState`() {
        val settings = InlayStringInterpolationSettings()
        val newState = InlayStringInterpolationState()
        newState.withStringInterpolationHint = false
        newState.withStringConstantHint = false

        settings.loadState(newState)

        assertFalse(settings.state.withStringInterpolationHint)
        assertFalse(settings.state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationSettings state persistence`() {
        val settings = InlayStringInterpolationSettings()

        settings.state.withStringInterpolationHint = false
        settings.state.withStringConstantHint = true

        val currentState = settings.state

        assertFalse(currentState.withStringInterpolationHint)
        assertTrue(currentState.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState multiple toggles`() {
        val state = InlayStringInterpolationState()

        // Toggle interpolation hint multiple times
        state.withStringInterpolationHint = false
        assertFalse(state.withStringInterpolationHint)

        state.withStringInterpolationHint = true
        assertTrue(state.withStringInterpolationHint)

        state.withStringInterpolationHint = false
        assertFalse(state.withStringInterpolationHint)
    }

    @Test
    fun `test InlayStringInterpolationState independent property changes`() {
        val state = InlayStringInterpolationState()

        state.withStringInterpolationHint = false
        assertFalse(state.withStringInterpolationHint)
        assertTrue(state.withStringConstantHint)

        state.withStringConstantHint = false
        assertFalse(state.withStringInterpolationHint)
        assertFalse(state.withStringConstantHint)

        state.withStringInterpolationHint = true
        assertTrue(state.withStringInterpolationHint)
        assertFalse(state.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState equals different values`() {
        val state1 = InlayStringInterpolationState()
        val state2 = InlayStringInterpolationState()

        assertEquals(state1.withStringInterpolationHint, state2.withStringInterpolationHint)
        assertEquals(state1.withStringConstantHint, state2.withStringConstantHint)
    }

    @Test
    fun `test InlayStringInterpolationState property modification isolation`() {
        val state1 = InlayStringInterpolationState()
        val state2 = InlayStringInterpolationState()

        state1.withStringInterpolationHint = false

        assertTrue(state2.withStringInterpolationHint)
        assertFalse(state1.withStringInterpolationHint)
    }
}
