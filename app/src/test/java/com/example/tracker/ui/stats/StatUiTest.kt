package com.example.tracker.ui.stats

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatUiTest {

    @Test
    fun sanitizeStatInput_keepsOneSeparatorAndTheStatsDecimals() {
        assertEquals("19.5", sanitizeStatInput("19,57", decimals = 1))
        assertEquals("19.57", sanitizeStatInput("19.5.7", decimals = 2))
        assertEquals("195", sanitizeStatInput("19.5", decimals = 0))
        assertEquals("-3.2", sanitizeStatInput("-3.2a", decimals = 1))
    }

    @Test
    fun parseStatInput_rejectsWhatIsNotANumber() {
        assertEquals(-3.2, parseStatInput("-3.2")!!, 0.0)
        assertNull(parseStatInput(""))
        assertNull(parseStatInput("-"))
        assertNull(parseStatInput("."))
    }

    @Test
    fun formatStatNumber_alwaysShowsTheDecimals() {
        assertEquals("1,250", formatStatNumber(1250.0, 0, Locale.US))
        assertEquals("19.0", formatStatNumber(19.0, 1, Locale.US))
        assertEquals("19.50", formatStatNumber(19.5, 2, Locale.US))
    }

    @Test
    fun toStatInput_hasNoGroupingAndRoundsFloatNoise() {
        assertEquals("1250", 1250.0.toStatInput(0))
        assertEquals("19.5", 19.499999.toStatInput(1))
        assertEquals("19.50", 19.5.toStatInput(2))
    }
}
