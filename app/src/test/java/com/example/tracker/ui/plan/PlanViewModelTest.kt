package com.example.tracker.ui.plan

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.PlanExercise
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakePlanRepository
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plans = FakePlanRepository()
    private val workouts = FakeWorkoutRepository()
    private val exercises = FakeExerciseRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))

    private val bench = Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS)
    private val press = Exercise("press", "Overhead Press", ExerciseType.WEIGHT_REPS)
    private val plank = Exercise("plank", "Plank", ExerciseType.DURATION)

    @Before
    fun setUp() {
        exercises.exercises.value = listOf(bench, press, plank)
        plans.plans.value = listOf(WorkoutPlan("push", "Push Day", 2, Instant.parse("2026-09-28T10:00:00Z")))
        setPlanExercises(bench, plank)
    }

    private fun setPlanExercises(vararg list: Exercise) {
        plans.planDetails.value = mapOf(
            "push" to PlanDetails(
                id = "push",
                name = "Push Day",
                exercises = list.mapIndexed { i, e -> PlanExercise("pe-${e.id}", e, i, targetSets = 3) },
            ),
        )
    }

    private fun createViewModel() = PlanViewModel("push", plans, workouts, exercises, time)

    private fun TestScope.collectState(viewModel: PlanViewModel): () -> PlanUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun PlanUiState.success() = this as PlanUiState.Success

    @Test
    fun uiState_listsExercisesInOrderWithMoveLimits() = runTest {
        val state = collectState(createViewModel()).invoke().success()

        assertEquals("Push Day", state.name)
        assertEquals(LocalDate.of(2026, 9, 28), state.lastDoneOn)
        assertEquals(
            listOf(
                PlanExerciseUi("pe-bench", "Bench Press", ExerciseType.WEIGHT_REPS, 3, canMoveUp = false, canMoveDown = true),
                PlanExerciseUi("pe-plank", "Plank", ExerciseType.DURATION, 3, canMoveUp = true, canMoveDown = false),
            ),
            state.exercises,
        )
        assertEquals(WorkoutActionUi.Start, state.workoutAction)
    }

    @Test
    fun uiState_offersOnlyLibraryExercisesNotInPlan() = runTest {
        val state = collectState(createViewModel()).invoke().success()

        assertEquals(listOf(ExerciseOptionUi("press", "Overhead Press", ExerciseType.WEIGHT_REPS)), state.availableExercises)
    }

    @Test
    fun uiState_emptyPlan_cannotStartWorkout() = runTest {
        setPlanExercises()
        val state = collectState(createViewModel()).invoke().success()

        assertTrue(state.exercises.isEmpty())
        assertEquals(WorkoutActionUi.Unavailable, state.workoutAction)
    }

    @Test
    fun uiState_workoutInProgress_offersResume() = runTest {
        workouts.activeWorkout.value = Workout("w1", "pull", "Pull Day", Instant.parse("2026-10-03T08:00:00Z"), null, emptyList())

        val state = collectState(createViewModel()).invoke().success()

        assertEquals(WorkoutActionUi.Resume("w1"), state.workoutAction)
    }

    @Test
    fun uiState_missingPlan_isNotFound() = runTest {
        plans.planDetails.value = emptyMap()

        assertEquals(PlanUiState.NotFound, collectState(createViewModel()).invoke())
    }

    @Test
    fun onMoveUpAndDown_moveWithinBounds() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onMoveUp(0) // Already first: ignored.
        viewModel.onMoveDown(0)
        viewModel.onMoveUp(1)
        viewModel.onMoveDown(1) // Already last: ignored.

        assertEquals(listOf("move push 0->1", "move push 1->0"), plans.writes)
    }

    @Test
    fun onRemoveExercise_removesAndOffersUndo() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onRemoveExercise("pe-plank")

        assertEquals(listOf("remove pe-plank"), plans.writes)
        assertEquals(PlanEvent.ExerciseRemoved("pe-plank", "Plank"), viewModel.events.first())

        viewModel.onUndoRemoveExercise("pe-plank")
        assertEquals("restore pe-plank", plans.writes.last())
    }

    @Test
    fun onAddExercise_addsToPlan() = runTest {
        createViewModel().onAddExercise("press")

        assertEquals(listOf("add push press"), plans.writes)
    }

    @Test
    fun onCreateExercise_createsTrimmedAndAddsToPlan() = runTest {
        val viewModel = createViewModel()

        viewModel.onCreateExercise("  Dips ", ExerciseType.WEIGHT_REPS)
        viewModel.onCreateExercise("   ", ExerciseType.DURATION)

        assertEquals(listOf("Dips" to ExerciseType.WEIGHT_REPS), exercises.createdExercises)
        assertEquals(listOf("add push exercise-1"), plans.writes)
    }

    @Test
    fun onRenamePlan_ignoresBlankNames() = runTest {
        val viewModel = createViewModel()

        viewModel.onRenamePlan(" ")
        viewModel.onRenamePlan(" Upper Body ")

        assertEquals(listOf("rename push Upper Body"), plans.writes)
    }

    @Test
    fun onDeletePlan_deletesAndCloses() = runTest {
        val viewModel = createViewModel()

        viewModel.onDeletePlan()

        assertEquals(listOf("delete push"), plans.writes)
        assertEquals(PlanEvent.PlanDeleted, viewModel.events.first())
    }

    @Test
    fun failedWrite_showsMessage() = runTest {
        plans.writeError = IllegalStateException("disk")
        val viewModel = createViewModel()

        viewModel.onDeletePlan()

        assertEquals(PlanEvent.ShowMessage(R.string.plan_error_delete), viewModel.events.first())
    }

    @Test
    fun onStartWorkout_opensWorkout() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onStartWorkout()

        assertEquals(listOf("push"), workouts.startedPlanIds)
        assertEquals(PlanEvent.OpenWorkout("workout-1"), viewModel.events.first())
        assertEquals(false, viewModel.uiState.value.success().isStartingWorkout)
    }

    @Test
    fun onStartWorkout_failure_showsMessage() = runTest {
        workouts.startWorkoutError = IllegalStateException("A workout is already in progress")
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onStartWorkout()

        assertEquals(PlanEvent.ShowMessage(R.string.plan_error_start_workout), viewModel.events.first())
        assertEquals(false, viewModel.uiState.value.success().isStartingWorkout)
    }
}
