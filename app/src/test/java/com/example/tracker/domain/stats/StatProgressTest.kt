package com.example.tracker.domain.stats

import com.example.tracker.domain.model.StatGoal
import org.junit.Assert.assertEquals
import org.junit.Test

class StatProgressTest {

    @Test
    fun change_goingDown_countsTheDropSoFar() {
        assertEquals(0.5f, progressToward(StatGoal.CHANGE, target = 15.0, first = 21.0, latest = 18.0), 1e-6f)
    }

    @Test
    fun change_goingUp_countsTheRiseSoFar() {
        assertEquals(0.25f, progressToward(StatGoal.CHANGE, target = 40.0, first = 32.0, latest = 34.0), 1e-6f)
    }

    @Test
    fun change_movingAwayOrOvershooting_staysWithinZeroAndOne() {
        assertEquals(0f, progressToward(StatGoal.CHANGE, target = 15.0, first = 21.0, latest = 23.0), 0f)
        assertEquals(1f, progressToward(StatGoal.CHANGE, target = 15.0, first = 21.0, latest = 14.0), 0f)
    }

    @Test
    fun change_startingOnTheTarget_isDone() {
        assertEquals(1f, progressToward(StatGoal.CHANGE, target = 15.0, first = 15.0, latest = 16.0), 0f)
    }

    @Test
    fun daily_exceedingTheTarget_isDoneNotZero() {
        // The reported bug: 110 steps against a 100-step target, logged as the first entry.
        assertEquals(1f, progressToward(StatGoal.DAILY, target = 100.0, first = 110.0, latest = 110.0), 0f)
    }

    @Test
    fun daily_isTheLatestValueAgainstTheTarget_whateverTheFirst() {
        assertEquals(0.5f, progressToward(StatGoal.DAILY, target = 10_000.0, first = 12_000.0, latest = 5_000.0), 1e-6f)
        assertEquals(1f, progressToward(StatGoal.DAILY, target = 10_000.0, first = 3_000.0, latest = 10_000.0), 0f)
    }

    @Test
    fun daily_nothingOrNegative_isZero() {
        assertEquals(0f, progressToward(StatGoal.DAILY, target = 100.0, first = 0.0, latest = 0.0), 0f)
        assertEquals(0f, progressToward(StatGoal.DAILY, target = 100.0, first = 0.0, latest = -5.0), 0f)
    }

    @Test
    fun daily_targetOfZero_isReachedByAnyValueAtOrAboveIt() {
        assertEquals(1f, progressToward(StatGoal.DAILY, target = 0.0, first = 0.0, latest = 0.0), 0f)
        assertEquals(0f, progressToward(StatGoal.DAILY, target = 0.0, first = 0.0, latest = -1.0), 0f)
    }
}
