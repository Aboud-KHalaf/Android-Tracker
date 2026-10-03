package com.example.tracker.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetInputTest {

    @Test
    fun parseWeight_acceptsDotOrCommaAndRejectsInvalid() {
        assertEquals(47.5, SetInput.parseWeight("47.5")!!, 0.0)
        assertEquals(47.5, SetInput.parseWeight(" 47,5 ")!!, 0.0)
        assertEquals(0.0, SetInput.parseWeight("0")!!, 0.0)
        assertNull(SetInput.parseWeight(""))
        assertNull(SetInput.parseWeight("-5"))
        assertNull(SetInput.parseWeight("abc"))
        assertNull(SetInput.parseWeight("5000"))
    }

    @Test
    fun parseReps_requiresAtLeastOne() {
        assertEquals(8, SetInput.parseReps("8"))
        assertNull(SetInput.parseReps("0"))
        assertNull(SetInput.parseReps("8.5"))
        assertNull(SetInput.parseReps(""))
    }

    @Test
    fun formatWeight_dropsTrailingZeros() {
        assertEquals("50", SetInput.formatWeight(50.0))
        assertEquals("47.5", SetInput.formatWeight(47.5))
        assertEquals("", SetInput.formatWeight(null))
    }

    @Test
    fun stepWeight_movesByTwoAndAHalfKgAndStopsAtZero() {
        assertEquals("50", SetInput.stepWeight("47.5", 1))
        assertEquals("45", SetInput.stepWeight("47,5", -1))
        assertEquals("2.5", SetInput.stepWeight("", 1))
        assertEquals("0", SetInput.stepWeight("1", -1))
    }

    @Test
    fun stepReps_movesByOneAndStopsAtOne() {
        assertEquals("9", SetInput.stepReps("8", 1))
        assertEquals("1", SetInput.stepReps("1", -1))
        assertEquals("1", SetInput.stepReps("", 1))
    }
}
