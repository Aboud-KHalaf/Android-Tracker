package com.example.tracker.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStatsTest {

    @Test
    fun stats_weightSession_computesTopSetRepsAndVolume() {
        val session = benchHistory.toSessions().last()

        val stats = session.stats()

        assertEquals(50.0, stats.topWeightKg!!, 0.0)
        assertEquals(8, stats.topSetReps)
        assertEquals(26, stats.totalReps)
        assertEquals(45.0 * 10 + 50.0 * 8 * 2, stats.volumeKg, 0.0)
        assertNull(stats.longestHoldSeconds)
    }

    @Test
    fun stats_durationSession_usesLongestHold() {
        val session = listOf(holdSet("w1", 0, 45), holdSet("w1", 0, 50), holdSet("w1", 0, 35))
            .toSessions().single()

        val stats = session.stats()

        assertEquals(50, stats.longestHoldSeconds)
        assertNull(stats.topWeightKg)
        assertEquals(0.0, stats.volumeKg, 0.0)
    }

    @Test
    fun toSessions_groupsByWorkoutAndExercise_oldestFirst() {
        val sets = listOf(
            weightSet("late", 5, 50.0, 5),
            weightSet("early", 1, 40.0, 5),
            weightSet("early", 1, 20.0, 10, exercise = "row"),
        )

        val sessions = sets.toSessions()

        assertEquals(listOf("early", "early", "late"), sessions.map { it.workoutId })
        assertEquals(setOf("bench", "row"), sessions.take(2).map { it.exerciseId }.toSet())
    }
}
