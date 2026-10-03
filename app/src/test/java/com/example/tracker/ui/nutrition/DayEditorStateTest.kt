package com.example.tracker.ui.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayEditorStateTest {

    @Test
    fun sanitizeAmount_keepsUpToFiveDigits() {
        assertEquals("2450", sanitizeAmount("2,450 kcal"))
        assertEquals("12345", sanitizeAmount("1234567"))
        assertEquals("", sanitizeAmount("-"))
    }

    @Test
    fun parseAmount_acceptsZeroToMax() {
        assertEquals(0, parseAmount("0", 100))
        assertEquals(100, parseAmount(" 100 ", 100))
        assertNull(parseAmount("101", 100))
        assertNull(parseAmount("", 100))
    }
}
