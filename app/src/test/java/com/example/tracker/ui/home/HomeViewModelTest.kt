package com.example.tracker.ui.home

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakePlanRepository
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.common.SetValueUi
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plans = FakePlanRepository()
    private val workouts = FakeWorkoutRepository()
    private val exercises = FakeExerciseRepository()

    // Saturday, October 3, 2026.
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))

    private fun createViewModel() = HomeViewModel(plans, workouts, exercises, time)

    /** Keeps [HomeViewModel.uiState] subscribed for the test and returns its current value. */
    private fun TestScope.collectState(viewModel: HomeViewModel): () -> HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun HomeUiState.success() = this as HomeUiState.Success

    @Test
    fun uiState_suggestsPlanDoneLongestAgo() = runTest {
        plans.plans.value = listOf(
            WorkoutPlan("push", "Push Day", 5, Instant.parse("2026-09-28T10:00:00Z")),
            WorkoutPlan("pull", "Pull Day", 5, Instant.parse("2026-09-30T10:00:00Z")),
        )
        val state = collectState(createViewModel())

        val success = state().success()
        assertEquals(LocalDate.of(2026, 10, 3), success.today)
        assertEquals(UpNextUi.Start("push", "Push Day", 5, LocalDate.of(2026, 9, 28)), success.upNext)
        assertEquals(listOf("Push Day", "Pull Day"), success.plans.map { it.name })
    }

    @Test
    fun uiState_workoutInProgress_offersResume() = runTest {
        plans.plans.value = listOf(WorkoutPlan("push", "Push Day", 5, null))
        workouts.activeWorkout.value = Workout(
            id = "w1", planId = "push", name = "Push Day",
            startedAt = Instant.parse("2026-10-03T08:30:00Z"), finishedAt = null, exercises = emptyList(),
        )
        val state = collectState(createViewModel())

        assertEquals(UpNextUi.Resume("w1", "Push Day", LocalTime.of(8, 30)), state().success().upNext)
    }

    @Test
    fun uiState_noPlans_isEmptyState() = runTest {
        val state = collectState(createViewModel())

        val success = state().success()
        assertEquals(UpNextUi.NoPlans, success.upNext)
        assertTrue(success.plans.isEmpty())
        assertTrue(success.recentWorkouts.isEmpty())
    }

    @Test
    fun uiState_weekStripMarksTrainedDaysAndToday() = runTest {
        workouts.weekSummary.value = WeekSummary(
            workoutCount = 2,
            totalDuration = Duration.ofMinutes(100),
            trainedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        val state = collectState(createViewModel())

        val week = state().success().week
        assertEquals(LocalDate.of(2026, 9, 28), workouts.requestedWeekStart)
        assertEquals(DayOfWeek.entries, week.days.map { it.dayOfWeek })
        assertEquals(DayStatus.TRAINED, week.days[0].status)
        assertEquals(DayStatus.IDLE, week.days[1].status)
        assertEquals(DayStatus.TRAINED, week.days[2].status)
        assertEquals(DayStatus.TODAY, week.days[5].status)
        assertEquals(2, week.workoutCount)
        assertEquals(Duration.ofMinutes(100), week.totalDuration)
    }

    @Test
    fun uiState_showsLatestPersonalBestFromThisWeekOnly() = runTest {
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS))
        workouts.personalBests.value = listOf(
            PersonalBest("old", "bench", ExerciseType.WEIGHT_REPS, Instant.parse("2026-09-21T10:00:00Z"), 45.0, 8, null),
            PersonalBest("new", "bench", ExerciseType.WEIGHT_REPS, Instant.parse("2026-09-28T10:00:00Z"), 50.0, 8, null),
        )
        val state = collectState(createViewModel())

        assertEquals(
            PersonalBestUi("bench", "Bench Press", SetValueUi.WeightReps(50.0, 8), DayOfWeek.MONDAY),
            state().success().week.personalBest,
        )

        workouts.personalBests.value = workouts.personalBests.value.take(1)
        assertNull(state().success().week.personalBest)
    }

    @Test
    fun uiState_listsAtMostThreeRecentWorkouts() = runTest {
        workouts.history.value = (1..5).map { day ->
            val start = Instant.parse("2026-09-2${day}T10:00:00Z")
            WorkoutSummary("w$day", "push", "Push Day", start, start.plus(Duration.ofMinutes(50)), 5, 0)
        }
        val state = collectState(createViewModel())

        val recent = state().success().recentWorkouts
        assertEquals(listOf("w1", "w2", "w3"), recent.map { it.id })
        assertEquals(Duration.ofMinutes(50), recent.first().duration)
        assertEquals(LocalDate.of(2026, 9, 21), recent.first().date)
    }

    @Test
    fun uiState_loadFailure_showsErrorUntilRetrySucceeds() = runTest {
        workouts.historyError = IllegalStateException("disk")
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        assertEquals(HomeUiState.Error, state())

        workouts.historyError = null
        viewModel.onRetry()

        assertTrue(state() is HomeUiState.Success)
    }

    @Test
    fun onStartWorkout_opensNewWorkout() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onStartWorkout("push")

        assertEquals(listOf("push"), workouts.startedPlanIds)
        assertEquals(HomeEvent.OpenWorkout("workout-1"), viewModel.events.first())
        assertEquals(false, viewModel.uiState.value.success().isStartingWorkout)
    }

    @Test
    fun onStartWorkout_failure_showsMessage() = runTest {
        workouts.startWorkoutError = IllegalArgumentException("unknown plan")
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onStartWorkout("push")

        assertEquals(HomeEvent.ShowMessage(R.string.home_error_start_workout), viewModel.events.first())
    }

    @Test
    fun onCreatePlan_trimsNameAndOpensPlan() = runTest {
        val viewModel = createViewModel()

        viewModel.onCreatePlan("  Push Day ")

        assertEquals(listOf("Push Day"), plans.createdPlanNames)
        assertEquals(HomeEvent.OpenPlan("plan-1"), viewModel.events.first())
    }

    @Test
    fun onCreatePlan_blankName_isIgnored() = runTest {
        createViewModel().onCreatePlan("   ")

        assertTrue(plans.createdPlanNames.isEmpty())
    }

    @Test
    fun onCreatePlan_failure_showsMessage() = runTest {
        plans.createPlanError = IllegalStateException("disk")
        val viewModel = createViewModel()

        viewModel.onCreatePlan("Push Day")

        assertEquals(HomeEvent.ShowMessage(R.string.home_error_create_plan), viewModel.events.first())
    }
}
