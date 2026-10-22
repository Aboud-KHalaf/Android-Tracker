package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutRecapTest {

    private val start = Instant.parse("2026-10-03T08:00:00Z")
    private val bench = Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS)
    private val row = Exercise("row", "Row", ExerciseType.WEIGHT_REPS)

    private fun set(id: String, kg: Double, reps: Int, done: Boolean = true) =
        WorkoutSet(id, 0, kg, reps, null, if (done) start else null)

    private fun workout(id: String, minutes: Long?, vararg exercises: WorkoutExercise) =
        Workout(id, "push", "Push", start, minutes?.let { start.plus(Duration.ofMinutes(it)) }, exercises.toList())

    private fun best(workoutId: String, exercise: Exercise) =
        PersonalBest(workoutId, exercise.id, exercise.type, start, 100.0, 5, null)

    @Test
    fun recapOf_countsOnlyCompletedSets() {
        val workout = workout(
            "w2", 50,
            WorkoutExercise("we1", bench, 0, listOf(set("a", 100.0, 5), set("b", 100.0, 5, done = false))),
            WorkoutExercise("we2", row, 1, listOf(set("c", 60.0, 10, done = false))),
        )

        val recap = recapOf(workout, emptyList(), previous = null)!!

        assertEquals(Duration.ofMinutes(50), recap.duration)
        assertEquals(1, recap.completedSetCount)
        assertEquals(500.0, recap.volumeKg, 0.0)
        assertEquals(listOf("bench"), recap.exercises.map { it.exercise.id })
        assertNull(recap.volumeChangeKg)
    }

    @Test
    fun recapOf_comparesVolumeWithThePreviousWorkout_andKeepsOnlyItsOwnBests() {
        val previous = workout("w1", 40, WorkoutExercise("we0", bench, 0, listOf(set("p", 90.0, 5))))
        val workout = workout("w2", 50, WorkoutExercise("we1", bench, 0, listOf(set("a", 100.0, 5))))

        val recap = recapOf(workout, listOf(best("w1", bench), best("w2", bench)), previous)!!

        assertEquals(50.0, recap.volumeChangeKg!!, 0.0)
        assertEquals(listOf("w2"), recap.personalBests.map { it.workoutId })
    }

    @Test
    fun recapOf_workoutInProgress_isNull() {
        assertNull(recapOf(workout("w", null), emptyList(), null))
    }
}
