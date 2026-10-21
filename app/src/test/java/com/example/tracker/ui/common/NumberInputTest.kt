package com.example.tracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberInputTest {

    @Test
    fun toWesternDigits_readsArabicAndPersianDigits() {
        assertEquals("72.5", "٧٢٫٥".toWesternDigits())
        assertEquals("1500", "۱۵۰۰".toWesternDigits())
        assertEquals("1250", "١٬٢٥٠".toWesternDigits())
    }

    @Test
    fun toWesternDigits_leavesOtherTextAlone() {
        assertEquals("47,5 kg", "47,5 kg".toWesternDigits())
    }
}
