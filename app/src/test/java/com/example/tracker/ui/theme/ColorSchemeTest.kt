package com.example.tracker.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/** Guards the brand color tokens from the Theme tokens artboard against drift. */
class ColorSchemeTest {

    @Test
    fun lightScheme_matchesBrandTokens() {
        assertEquals(Color(0xFF2A5FA8), LightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), LightColorScheme.onPrimary)
        assertEquals(Color(0xFFF9F9FF), LightColorScheme.surface)
        assertEquals(Color(0xFFFFDCBE), LightColorScheme.tertiaryContainer)
    }

    @Test
    fun darkScheme_matchesBrandTokens() {
        assertEquals(Color(0xFFAAC7FF), DarkColorScheme.primary)
        assertEquals(Color(0xFF0A305F), DarkColorScheme.onPrimary)
        assertEquals(Color(0xFF111318), DarkColorScheme.surface)
        assertEquals(Color(0xFF693C00), DarkColorScheme.tertiaryContainer)
    }
}
