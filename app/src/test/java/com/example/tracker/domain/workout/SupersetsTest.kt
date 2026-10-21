package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SupersetsTest {

    @Test
    fun runOf_findsTheNeighboursWithTheSameGroup() {
        val groups = listOf(null, "a", "a", "a", "b", "b", null)

        assertEquals(0..0, Supersets.runOf(groups, 0))
        assertEquals(1..3, Supersets.runOf(groups, 2))
        assertEquals(4..5, Supersets.runOf(groups, 4))
    }

    @Test
    fun runOf_sameGroupApartIsNotOneSuperset() {
        val groups = listOf("a", null, "a")

        assertEquals(0..0, Supersets.runOf(groups, 0))
        assertEquals(emptyList<IntRange>(), Supersets.runs(groups))
    }

    @Test
    fun runs_listsSupersetsOfTwoOrMore() {
        assertEquals(listOf(1..2, 3..4), Supersets.runs(listOf(null, "a", "a", "b", "b", "c")))
    }

    private val done = Instant.parse("2026-10-03T08:35:00Z")
    private val times = RestTimes(betweenSetsSeconds = 90, betweenExercisesSeconds = 120)

    private fun exercise(id: String, position: Int, group: String?, vararg completed: Boolean) = WorkoutExercise(
        id = "we-$id",
        exercise = Exercise(id, id, ExerciseType.WEIGHT_REPS),
        position = position,
        sets = completed.mapIndexed { i, isDone -> WorkoutSet("$id-$i", i, 50.0, 8, null, if (isDone) done else null) },
        supersetGroup = group,
    )

    private fun workout(vararg exercises: WorkoutExercise) =
        Workout("w1", "push", "Push", done, null, exercises.toList())

    @Test
    fun afterCompleting_inASuperset_goesToTheNextExerciseWithoutRest() {
        val workout = workout(exercise("bench", 0, "s", true, false), exercise("row", 1, "s", false, false))

        assertEquals(AfterSet(1, null), workout.afterCompleting("bench-0", times))
    }

    @Test
    fun afterCompleting_endOfARound_restsAndGoesBackToTheFirst() {
        val workout = workout(exercise("bench", 0, "s", true, false), exercise("row", 1, "s", true, false))

        assertEquals(AfterSet(0, RestPeriod(RestKind.BETWEEN_SETS, 90)), workout.afterCompleting("row-0", times))
    }

    @Test
    fun afterCompleting_partnerHasNoSetsLeft_staysAndRests() {
        val workout = workout(exercise("bench", 0, "s", true), exercise("row", 1, "s", true, true, false))

        assertEquals(AfterSet(1, RestPeriod(RestKind.BETWEEN_SETS, 90)), workout.afterCompleting("row-1", times))
    }

    @Test
    fun afterCompleting_supersetDone_restsBeforeTheNextExercise() {
        val workout = workout(
            exercise("bench", 0, "s", true),
            exercise("row", 1, "s", true),
            exercise("curl", 2, null, false),
        )

        assertEquals(AfterSet(null, RestPeriod(RestKind.BETWEEN_EXERCISES, 120)), workout.afterCompleting("row-0", times))
    }

    @Test
    fun afterCompleting_outsideASuperset_isTheUsualRest() {
        val workout = workout(exercise("bench", 0, null, true, false), exercise("row", 1, null, false))

        assertEquals(AfterSet(null, RestPeriod(RestKind.BETWEEN_SETS, 90)), workout.afterCompleting("bench-0", times))
    }

    @Test
    fun afterCompleting_endOfARoundWithRestOff_movesOnWithoutRest() {
        val workout = workout(exercise("bench", 0, "s", true, false), exercise("row", 1, "s", true, false))

        assertEquals(AfterSet(0, null), workout.afterCompleting("row-0", RestTimes(0, 0)))
    }
}
