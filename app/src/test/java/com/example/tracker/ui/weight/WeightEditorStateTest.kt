package com.example.tracker.ui.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightEditorStateTest {

    @Test
    fun sanitizeWeight_keepsOneSeparatorAndOneDecimal() {
        assertEquals("78.4", sanitizeWeight("78,45"))
        assertEquals("78.", sanitizeWeight("78.."))
        assertEquals("100.5", sanitizeWeight("100.5 kg"))
        assertEquals("1234.", sanitizeWeight("1234.5"))
    }

    @Test
    fun parseWeight_acceptsOnlyTheAllowedRange() {
        assertEquals(78.4, parseWeight("78.4")!!, 0.0)
        assertEquals(20.0, parseWeight("20")!!, 0.0)
        assertNull(parseWeight("19.9"))
        assertNull(parseWeight("400.1"))
        assertNull(parseWeight(""))
    }

    @Test
    fun toWeightInput_dropsTrailingZero() {
        assertEquals("80", 80.0.toWeightInput())
        assertEquals("79.5", 79.5.toWeightInput())
    }
}
