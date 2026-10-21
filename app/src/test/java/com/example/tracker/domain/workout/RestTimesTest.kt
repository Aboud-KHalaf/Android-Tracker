package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimesTest {

    private val done = Instant.parse("2026-10-03T08:35:00Z")
    private val times = RestTimes(betweenSetsSeconds = 90, betweenExercisesSeconds = 120)

    private fun exercise(id: String, position: Int, vararg completed: Boolean) = WorkoutExercise(
        id = "we-$id",
        exercise = Exercise(id, id, ExerciseType.WEIGHT_REPS),
        position = position,
        sets = completed.mapIndexed { i, isDone -> WorkoutSet("$id-$i", i, 50.0, 8, null, if (isDone) done else null) },
    )

    private fun workout(vararg exercises: WorkoutExercise) =
        Workout("w1", "push", "Push", done, null, exercises.toList())

    @Test
    fun setsLeftInTheExercise_restBetweenSets() {
        val workout = workout(exercise("bench", 0, true, false), exercise("curl", 1, false))

        assertEquals(RestPeriod(RestKind.BETWEEN_SETS, 90), workout.restAfterCompleting("bench-0", times))
    }

    @Test
    fun lastSetOfTheExercise_restBetweenExercises() {
        val workout = workout(exercise("bench", 0, true, true), exercise("curl", 1, false))

        assertEquals(RestPeriod(RestKind.BETWEEN_EXERCISES, 120), workout.restAfterCompleting("bench-1", times))
    }

    @Test
    fun anOpenEarlierExercise_alsoCountsAsSomethingLeft() {
        val workout = workout(exercise("bench", 0, false), exercise("curl", 1, true))

        assertEquals(RestKind.BETWEEN_EXERCISES, workout.restAfterCompleting("curl-0", times)?.kind)
    }

    @Test
    fun nothingLeftToDo_noRest() {
        val workout = workout(exercise("bench", 0, true), exercise("curl", 1, true))

        assertNull(workout.restAfterCompleting("curl-0", times))
    }

    @Test
    fun restTurnedOff_noRest() {
        val workout = workout(exercise("bench", 0, true, false))

        assertNull(workout.restAfterCompleting("bench-0", times.copy(betweenSetsSeconds = 0)))
    }

    @Test
    fun unknownSet_noRest() {
        assertNull(workout(exercise("bench", 0, false)).restAfterCompleting("other", times))
    }
}
