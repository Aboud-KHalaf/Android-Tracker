package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.testing.FakeWorkoutRepository
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutSessionTest {

    private val started = Instant.parse("2026-10-03T08:30:00Z")
    private val done = Instant.parse("2026-10-03T08:35:00Z")

    private fun exercise(id: String, position: Int, vararg completed: Boolean) = WorkoutExercise(
        id = "we-$id",
        exercise = Exercise(id, id.replaceFirstChar { it.uppercase() }, ExerciseType.WEIGHT_REPS),
        position = position,
        sets = completed.mapIndexed { i, isDone -> WorkoutSet("$id-$i", i, 50.0, 8, null, if (isDone) done else null) },
    )

    private val workout = Workout(
        id = "w1",
        planId = "push",
        name = "Push",
        startedAt = started,
        finishedAt = null,
        exercises = listOf(exercise("bench", 0, true, true), exercise("dips", 1, true, false), exercise("curl", 2, false)),
    )

    private class RecordingNotifier : ActiveWorkoutNotifier {
        val shown = mutableListOf<ActiveWorkoutStatus>()
        var cancelCount = 0

        override fun show(status: ActiveWorkoutStatus) {
            shown += status
        }

        override fun cancel() {
            cancelCount++
        }
    }

    @Test
    fun resumeExerciseIndex_prefersTheStoredOne_elseTheFirstWithOpenSets() {
        assertEquals(1, workout.resumeExerciseIndex())
        assertEquals(2, workout.copy(currentExerciseIndex = 2).resumeExerciseIndex())
        // An index that no longer exists falls back.
        assertEquals(1, workout.copy(currentExerciseIndex = 7).resumeExerciseIndex())
    }

    @Test
    fun resumeExerciseIndex_allDone_isTheLastExercise() {
        val finished = workout.copy(exercises = listOf(exercise("bench", 0, true), exercise("dips", 1, true)))

        assertEquals(1, finished.resumeExerciseIndex())
    }

    @Test
    fun toStatus_summarizesProgressFromTheStartTime() {
        assertEquals(
            ActiveWorkoutStatus("w1", "Push", started, currentExerciseName = "Dips", completedSets = 3, totalSets = 5),
            workout.toStatus(),
        )
    }

    @Test
    fun keepInSync_followsTheWorkoutUntilItEnds() = runTest {
        val workouts = FakeWorkoutRepository(now = { done })
        val notifier = RecordingNotifier()
        workouts.activeWorkout.value = workout
        ActiveWorkoutSession(workouts, notifier).keepInSync(backgroundScope)
        runCurrent()

        workouts.completeSet("dips-1")
        runCurrent()
        workouts.finishWorkout("w1")
        runCurrent()

        assertEquals(listOf(3, 4), notifier.shown.map { it.completedSets })
        assertEquals(1, notifier.cancelCount)
    }

    @Test
    fun syncNow_restoresTheNotificationOrRemovesAStaleOne() = runTest(UnconfinedTestDispatcher()) {
        val workouts = FakeWorkoutRepository()
        val notifier = RecordingNotifier()
        val session = ActiveWorkoutSession(workouts, notifier)

        session.syncNow()
        workouts.activeWorkout.value = workout
        session.syncNow()

        assertEquals(1, notifier.cancelCount)
        assertEquals(listOf("w1"), notifier.shown.map { it.workoutId })
    }

    @Test
    fun keepInSync_survivesAFailingDatabase() = runTest {
        val workouts = object : WorkoutRepository by FakeWorkoutRepository() {
            override fun observeActiveWorkout() = flow<Workout?> { throw IllegalStateException("disk") }
        }

        val job = ActiveWorkoutSession(workouts, RecordingNotifier()).keepInSync(backgroundScope)
        runCurrent()

        assertTrue(job.isCompleted)
    }
}
