package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.WorkoutSet
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SetRecordsTest {

    private val start = Instant.parse("2026-10-03T08:00:00Z")

    private fun set(id: String, kg: Double?, minute: Long?, seconds: Int? = null) =
        WorkoutSet(id, 0, kg, if (kg != null) 5 else null, seconds, minute?.let { start.plusSeconds(it * 60) })

    @Test
    fun recordSetIds_eachSetThatBeatsTheBestSoFar() {
        val sets = listOf(set("a", 100.0, 1), set("b", 102.5, 2), set("c", 101.0, 3), set("d", 105.0, 4))

        assertEquals(setOf("b", "d"), recordSetIds(ExerciseType.WEIGHT_REPS, sets, previousBestValue = 100.0))
    }

    @Test
    fun recordSetIds_followsCompletionOrderAndSkipsOpenSets() {
        val sets = listOf(set("late", 110.0, 5), set("early", 108.0, 1), set("open", 120.0, null))

        assertEquals(setOf("early", "late"), recordSetIds(ExerciseType.WEIGHT_REPS, sets, previousBestValue = 100.0))
    }

    @Test
    fun recordSetIds_firstTime_noRecords() {
        assertEquals(emptySet<String>(), recordSetIds(ExerciseType.WEIGHT_REPS, listOf(set("a", 50.0, 1)), null))
    }

    @Test
    fun recordSetIds_holdsCompareTheirTime() {
        val sets = listOf(set("a", null, 1, seconds = 60), set("b", null, 2, seconds = 75))

        assertEquals(setOf("b"), recordSetIds(ExerciseType.DURATION, sets, previousBestValue = 60.0))
    }

    @Test
    fun bestValue_isTheTopWeightOfTheBestSession() {
        assertEquals(50.0, bestValue(ExerciseType.WEIGHT_REPS, benchHistory.toSessions())!!, 0.0)
        assertEquals(null, bestValue(ExerciseType.WEIGHT_REPS, emptyList()))
    }
}
