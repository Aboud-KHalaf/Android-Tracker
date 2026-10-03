package com.example.tracker.domain.progress

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetImprovementTest {

    private fun set(kg: Double? = null, reps: Int? = null, seconds: Int? = null) =
        WorkoutSet("s", 0, kg, reps, seconds, null)

    @Test
    fun heavierWeight_isAWeightImprovement() {
        val result = improvementOver(ExerciseType.WEIGHT_REPS, set(47.5, 10), set(45.0, 10))

        assertEquals(SetImprovement.Weight(2.5), result)
    }

    @Test
    fun sameWeightMoreReps_isARepsImprovement() {
        val result = improvementOver(ExerciseType.WEIGHT_REPS, set(50.0, 9), set(50.0, 8))

        assertEquals(SetImprovement.Reps(1), result)
    }

    @Test
    fun lighterWeightMoreReps_isNotAnImprovement() {
        assertNull(improvementOver(ExerciseType.WEIGHT_REPS, set(45.0, 12), set(50.0, 8)))
    }

    @Test
    fun longerHold_isAHoldImprovement() {
        val result = improvementOver(ExerciseType.DURATION, set(seconds = 50), set(seconds = 45))

        assertEquals(SetImprovement.Hold(5), result)
    }

    @Test
    fun noPreviousSet_isNull() {
        assertNull(improvementOver(ExerciseType.WEIGHT_REPS, set(50.0, 8), null))
    }
}
