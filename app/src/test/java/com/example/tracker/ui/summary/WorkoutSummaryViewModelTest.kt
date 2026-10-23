package com.example.tracker.ui.summary

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.common.SetValueUi
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSummaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val workouts = FakeWorkoutRepository()
    private val started = Instant.parse("2026-10-03T08:00:00Z")
    private val bench = Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS)

    private fun finishedWorkout(finished: Boolean = true) = Workout(
        id = "w1",
        planId = "push",
        name = "Push Day",
        startedAt = started,
        finishedAt = if (finished) started.plus(Duration.ofMinutes(45)) else null,
        exercises = listOf(
            WorkoutExercise("we", bench, 0, listOf(WorkoutSet("s1", 0, 100.0, 5, null, started))),
        ),
    )

    private fun TestScope.stateOf(viewModel: WorkoutSummaryViewModel): () -> WorkoutSummaryUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    @Test
    fun finishedWorkout_isSummedUpWithItsPersonalBests() = runTest {
        workouts.activeWorkout.value = finishedWorkout()
        workouts.personalBests.value = listOf(PersonalBest("w1", "bench", ExerciseType.WEIGHT_REPS, started, 100.0, 5, null))

        val state = stateOf(WorkoutSummaryViewModel("w1", justFinished = true, workouts, time))()

        assertEquals(
            WorkoutSummaryUiState.Success(
                name = "Push Day",
                date = LocalDate.of(2026, 10, 3),
                justFinished = true,
                duration = Duration.ofMinutes(45),
                setCount = 1,
                volumeKg = 500.0,
                volumeChangeKg = null,
                personalBests = listOf(PersonalBestUi("Bench Press", SetValueUi.WeightReps(100.0, 5))),
                exercises = listOf(ExerciseSummaryUi("bench", "Bench Press", listOf(SetValueUi.WeightReps(100.0, 5)), true)),
            ),
            state,
        )
    }

    @Test
    fun notes_areShownWithTheWorkoutAndEachExercise() = runTest {
        val plank = Exercise("plank", "Plank", ExerciseType.DURATION)
        workouts.activeWorkout.value = finishedWorkout().let { workout ->
            workout.copy(
                note = "Felt strong",
                exercises = listOf(
                    workout.exercises.single().copy(note = "Pause at the chest"),
                    // Skipped, but its note is still worth keeping.
                    WorkoutExercise("wp", plank, 1, listOf(WorkoutSet("s2", 0, null, null, 60, null)), note = "Elbow hurt"),
                ),
            )
        }

        val state = stateOf(WorkoutSummaryViewModel("w1", justFinished = false, workouts, time))() as WorkoutSummaryUiState.Success

        assertEquals("Felt strong", state.note)
        assertEquals(listOf("Pause at the chest", "Elbow hurt"), state.exercises.map { it.note })
        assertEquals(listOf(emptyList<SetValueUi>()), state.exercises.drop(1).map { it.sets })
        assertEquals(1, state.setCount)
    }

    @Test
    fun workoutInProgress_isNotFound() = runTest {
        workouts.activeWorkout.value = finishedWorkout(finished = false)

        val state = stateOf(WorkoutSummaryViewModel("w1", justFinished = false, workouts, time))()

        assertEquals(WorkoutSummaryUiState.NotFound, state)
    }
}
