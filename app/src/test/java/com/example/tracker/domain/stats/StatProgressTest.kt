package com.example.tracker.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class StatProgressTest {

    @Test
    fun goingDown_countsTheDropSoFar() {
        assertEquals(0.5f, progressToward(target = 15.0, first = 21.0, latest = 18.0), 1e-6f)
    }

    @Test
    fun goingUp_countsTheRiseSoFar() {
        assertEquals(0.25f, progressToward(target = 40.0, first = 32.0, latest = 34.0), 1e-6f)
    }

    @Test
    fun movingAwayOrOvershooting_staysWithinZeroAndOne() {
        assertEquals(0f, progressToward(target = 15.0, first = 21.0, latest = 23.0), 0f)
        assertEquals(1f, progressToward(target = 15.0, first = 21.0, latest = 14.0), 0f)
    }

    @Test
    fun startingOnTheTarget_isDone() {
        assertEquals(1f, progressToward(target = 15.0, first = 15.0, latest = 16.0), 0f)
    }
}
