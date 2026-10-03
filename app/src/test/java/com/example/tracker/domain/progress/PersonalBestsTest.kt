package com.example.tracker.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalBestsTest {

    @Test
    fun findPersonalBests_flagsSessionsThatBeatEveryEarlierTopWeight() {
        val bests = findPersonalBests(benchHistory.toSessions())

        assertEquals(listOf("w3", "w5", "w6"), bests.map { it.workoutId })
        assertEquals(50.0, bests.last().weightKg!!, 0.0)
        assertEquals(8, bests.last().reps)
    }

    @Test
    fun findPersonalBests_firstSessionIsOnlyABaseline() {
        val bests = findPersonalBests(listOf(weightSet("w1", 0, 100.0, 5)).toSessions())

        assertTrue(bests.isEmpty())
    }

    @Test
    fun findPersonalBests_matchingThePreviousBestIsNotAPersonalBest() {
        val sets = listOf(weightSet("w1", 0, 50.0, 5), weightSet("w2", 1, 50.0, 8))

        assertTrue(findPersonalBests(sets.toSessions()).isEmpty())
    }

    @Test
    fun findPersonalBests_durationExercise_comparesLongestHold() {
        val sets = listOf(holdSet("w1", 0, 40), holdSet("w2", 1, 45), holdSet("w3", 2, 30))

        val bests = findPersonalBests(sets.toSessions())

        assertEquals(listOf("w2"), bests.map { it.workoutId })
        assertEquals(45, bests.single().durationSeconds)
        assertNull(bests.single().weightKg)
    }

    @Test
    fun findPersonalBests_tracksEachExerciseSeparately() {
        val sets = listOf(
            weightSet("w1", 0, 100.0, 5, exercise = "squat"),
            weightSet("w1", 0, 50.0, 5, exercise = "bench"),
            weightSet("w2", 1, 55.0, 5, exercise = "bench"),
        )

        val bests = findPersonalBests(sets.toSessions())

        assertEquals(listOf("bench"), bests.map { it.exerciseId })
    }

    @Test
    fun currentPersonalBest_returnsEarliestSessionWithTheBestValue() {
        val sets = listOf(weightSet("w1", 0, 60.0, 3), weightSet("w2", 1, 60.0, 5), weightSet("w3", 2, 55.0, 8))

        val best = currentPersonalBest(sets.toSessions())

        assertEquals("w1", best?.workoutId)
    }

    @Test
    fun currentPersonalBest_noSessions_isNull() {
        assertNull(currentPersonalBest(emptyList()))
    }
}
