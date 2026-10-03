package com.example.tracker.ui.workout

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.SetImprovement
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.common.SetValueUi
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val workouts = FakeWorkoutRepository(now = { time.now() })
    private val started = Instant.parse("2026-10-03T08:30:00Z")
    private val done = Instant.parse("2026-10-03T08:35:00Z")

    private val bench = Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS)
    private val incline = Exercise("incline", "Incline Dumbbell Press", ExerciseType.WEIGHT_REPS)

    private fun set(id: String, position: Int, kg: Double?, reps: Int?, completed: Boolean = false) =
        WorkoutSet(id, position, kg, reps, null, if (completed) done else null)

    @Before
    fun setUp() {
        workouts.activeWorkout.value = Workout(
            id = "w1",
            planId = "push",
            name = "Push Day",
            startedAt = started,
            finishedAt = null,
            exercises = listOf(
                WorkoutExercise(
                    "we-bench", bench, 0,
                    listOf(set("s1", 0, 47.5, 10, completed = true), set("s2", 1, 50.0, 8), set("s3", 2, 50.0, 8)),
                ),
                WorkoutExercise("we-incline", incline, 1, listOf(set("s4", 0, 20.0, 10), set("s5", 1, 20.0, 10))),
            ),
        )
        workouts.lastTimes["bench"] = listOf(set("p1", 0, 45.0, 10), set("p2", 1, 50.0, 8), set("p3", 2, 50.0, 8))
    }

    private fun createViewModel() = ActiveWorkoutViewModel("w1", workouts, time)

    private fun TestScope.collectState(viewModel: ActiveWorkoutViewModel): () -> ActiveWorkoutUiState.Success {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value as ActiveWorkoutUiState.Success }
    }

    private fun ActiveWorkoutUiState.Success.active() = sets.filterIsInstance<SetRowUi.Active>().single()
    private fun ActiveWorkoutUiState.Success.editor() = active().editor as SetEditorUi.WeightReps

    @Test
    fun uiState_showsFirstOpenExerciseWithSetStates() = runTest {
        val state = collectState(createViewModel())()

        assertEquals("Push Day", state.workoutName)
        assertEquals(started, state.startedAt)
        assertEquals(0, state.exerciseIndex)
        assertEquals(2, state.exerciseCount)
        assertEquals("Bench Press", state.exercise.name)
        assertEquals(
            listOf(SetValueUi.WeightReps(45.0, 10), SetValueUi.WeightReps(50.0, 8), SetValueUi.WeightReps(50.0, 8)),
            state.exercise.lastTime,
        )
        assertEquals(
            listOf(
                SetRowUi.Done("s1", 1, SetValueUi.WeightReps(47.5, 10), SetImprovement.Weight(2.5)),
                SetRowUi.Active("s2", 2, SetValueUi.WeightReps(50.0, 8), SetEditorUi.WeightReps("50", "8")),
                SetRowUi.Upcoming("s3", 3, SetValueUi.WeightReps(50.0, 8)),
            ),
            state.sets,
        )
        assertNull(state.previousExerciseName)
        assertEquals("Incline Dumbbell Press", state.nextExerciseName)
        assertEquals(1, state.completedSetCount)
        assertEquals(4, state.openSetCount)
    }

    @Test
    fun steppersAndTyping_editTheDraftOnly() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onWeightStep(1)
        viewModel.onRepsStep(-1)
        assertEquals(SetEditorUi.WeightReps("52.5", "7"), state().editor())

        viewModel.onWeightTextChange("55,")
        assertEquals("55,", state().editor().weightText)
        assertEquals(50.0, workouts.set("s2").weightKg!!, 0.0)
    }

    @Test
    fun onCompleteSet_savesValuesMovesOnAndOffersUndo() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)
        viewModel.onWeightStep(1)

        viewModel.onCompleteSet()

        val saved = workouts.set("s2")
        assertEquals(52.5, saved.weightKg!!, 0.0)
        assertTrue(saved.isCompleted)
        assertEquals("s3", state().active().id)
        assertEquals(ActiveWorkoutEvent.SetCompleted("s2", 2), viewModel.events.first())

        viewModel.onUndoCompleteSet("s2")
        assertTrue(!workouts.set("s2").isCompleted)
        assertEquals("s2", state().active().id)
    }

    @Test
    fun onCompleteSet_invalidInput_isIgnored() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onRepsTextChange("0")
        assertTrue(!state().editor().canComplete)
        viewModel.onCompleteSet()

        assertTrue(!workouts.set("s2").isCompleted)
    }

    @Test
    fun completingLastSet_staysOnExercise() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onCompleteSet()
        viewModel.onCompleteSet()

        assertEquals(0, state().exerciseIndex)
        assertTrue(state().allSetsDone)
    }

    @Test
    fun onSelectSet_doneSetIsReopenedAndDraftOfPreviousSetSaved() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)
        viewModel.onRepsTextChange("9")

        viewModel.onSelectSet("s1")

        assertEquals(9, workouts.set("s2").reps)
        assertTrue(!workouts.set("s1").isCompleted)
        assertEquals("s1", state().active().id)
    }

    @Test
    fun onAddSet_andRemoveActiveSet() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onAddSet()
        assertEquals(4, state().sets.size)
        assertEquals(SetValueUi.WeightReps(50.0, 8), (state().sets.last() as SetRowUi.Upcoming).planned)

        viewModel.onRemoveActiveSet()
        assertEquals(listOf("s1", "s3", "new-set-1"), state().sets.map { it.id })
        assertEquals("s3", state().active().id)
    }

    @Test
    fun exerciseNavigation_staysInBounds() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onPreviousExercise()
        assertEquals(0, state().exerciseIndex)

        viewModel.onNextExercise()
        assertEquals(1, state().exerciseIndex)
        assertEquals("Bench Press", state().previousExerciseName)
        assertNull(state().nextExerciseName)
        assertTrue(state().exercise.lastTime.isEmpty())

        viewModel.onNextExercise()
        assertEquals(1, state().exerciseIndex)
    }

    @Test
    fun onFinishWorkout_finishesAndEnds() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onFinishWorkout()

        assertNotNull(workouts.activeWorkout.value?.finishedAt)
        assertEquals(ActiveWorkoutEvent.WorkoutEnded, viewModel.events.first())
        assertEquals(ActiveWorkoutUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun onDiscardWorkout_removesWorkout() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onDiscardWorkout()

        assertNull(workouts.activeWorkout.value)
        assertEquals(ActiveWorkoutEvent.WorkoutEnded, viewModel.events.first())
    }

    @Test
    fun failedSave_showsMessage() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)
        workouts.writeError = IllegalStateException("disk")

        viewModel.onCompleteSet()

        assertEquals(ActiveWorkoutEvent.ShowMessage(R.string.workout_error_save), viewModel.events.first())
    }

    // --- Timed (duration) sets ---

    private val plank = Exercise("plank", "Plank", ExerciseType.DURATION)

    /** A Plank-only workout: set h1 done at 0:50, h2 and h3 open, prefilled from last time. */
    private fun usePlankWorkout() {
        workouts.activeWorkout.value = workouts.activeWorkout.value!!.copy(
            exercises = listOf(
                WorkoutExercise(
                    "we-plank", plank, 0,
                    listOf(
                        WorkoutSet("h1", 0, null, null, 50, done),
                        WorkoutSet("h2", 1, null, null, 40, null),
                        WorkoutSet("h3", 2, null, null, 35, null),
                    ),
                ),
            ),
        )
        workouts.lastTimes["plank"] = listOf(
            WorkoutSet("q1", 0, null, null, 45, done),
            WorkoutSet("q2", 1, null, null, 40, done),
            WorkoutSet("q3", 2, null, null, 35, done),
        )
    }

    private fun ActiveWorkoutUiState.Success.hold() = active().editor as SetEditorUi.Duration

    @Test
    fun durationSet_startsFromZeroWithLastTimeAsTarget() = runTest {
        usePlankWorkout()
        val state = collectState(createViewModel())()

        assertEquals(SetRowUi.Done("h1", 1, SetValueUi.Hold(50), SetImprovement.Hold(5)), state.sets[0])
        assertEquals(SetEditorUi.Duration(bankedSeconds = 0, runningSince = null, targetSeconds = 40), state.hold())
        assertEquals(HoldStatus.BelowTarget(40), state.hold().statusAt(time.now()))
    }

    @Test
    fun timer_runsPausesAndAdjusts() = runTest {
        usePlankWorkout()
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onToggleTimer()
        assertTrue(state().hold().isRunning)
        time.advance(Duration.ofSeconds(30))
        assertEquals(30, state().hold().secondsAt(time.now()))

        viewModel.onToggleTimer()
        time.advance(Duration.ofSeconds(10)) // Paused: doesn't count.
        assertEquals(SetEditorUi.Duration(30, null, 40), state().hold())

        viewModel.onAdjustHold(15)
        assertEquals(HoldStatus.PastTarget(5), state().hold().statusAt(time.now()))
        viewModel.onAdjustHold(-60)
        assertEquals(0, state().hold().secondsAt(time.now()))
    }

    @Test
    fun adjustingWhileRunning_keepsRunning() = runTest {
        usePlankWorkout()
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onToggleTimer()
        time.advance(Duration.ofSeconds(20))
        viewModel.onAdjustHold(5)
        time.advance(Duration.ofSeconds(15))

        assertTrue(state().hold().isRunning)
        assertEquals(40, state().hold().secondsAt(time.now()))
        assertEquals(HoldStatus.MatchedTarget, state().hold().statusAt(time.now()))
    }

    @Test
    fun completeHold_savesElapsedSecondsAndMovesOn() = runTest {
        usePlankWorkout()
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onCompleteSet() // 0 s: nothing to log.
        assertTrue(!workouts.set("h2").isCompleted)

        viewModel.onToggleTimer()
        time.advance(Duration.ofSeconds(42))
        viewModel.onCompleteSet()

        assertEquals(42, workouts.set("h2").durationSeconds)
        assertTrue(workouts.set("h2").isCompleted)
        assertEquals(SetRowUi.Done("h2", 2, SetValueUi.Hold(42), SetImprovement.Hold(2)), state().sets[1])
        assertEquals("h3", state().active().id)
        assertEquals(0, state().hold().bankedSeconds)
    }

    @Test
    fun reopenedHold_continuesFromRecordedTime() = runTest {
        usePlankWorkout()
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSelectSet("h1")

        assertEquals("h1", state().active().id)
        assertEquals(50, state().hold().bankedSeconds)
    }

    @Test
    fun switchingSets_savesRunningHold() = runTest {
        usePlankWorkout()
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onToggleTimer()
        time.advance(Duration.ofSeconds(25))
        viewModel.onSelectSet("h3")

        assertEquals(25, workouts.set("h2").durationSeconds)
        assertTrue(!workouts.set("h2").isCompleted)
    }
}
