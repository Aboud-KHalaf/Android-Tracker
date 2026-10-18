package com.example.tracker.ui.home

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakeProgramRepository
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.program.ProgramSetup
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
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

    private val programs = FakeProgramRepository()
    private val workouts = FakeWorkoutRepository()
    private val exercises = FakeExerciseRepository()

    // Saturday, October 3, 2026.
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))

    private fun createViewModel() = HomeViewModel(programs, workouts, exercises, time, locale = { Locale.US })

    /** Keeps [HomeViewModel.uiState] subscribed for the test and returns its current value. */
    private fun TestScope.collectState(viewModel: HomeViewModel): () -> HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun HomeUiState.success() = this as HomeUiState.Success

    private fun day(id: String, weekday: DayOfWeek?, exercises: Int = 5, lastDoneAt: Instant? = null) =
        WorkoutPlan(id, id.replaceFirstChar { it.uppercase() }, exercises, lastDoneAt, programId = "ppl", dayOfWeek = weekday)

    private fun program(id: String, isActive: Boolean, vararg days: WorkoutPlan) =
        Program(id, id.uppercase(), isActive, days.toList())

    @Test
    fun uiState_upNextIsTodaysDayOfTheActiveProgram() = runTest {
        programs.programs.value = listOf(
            program("ppl", isActive = true, day("push", DayOfWeek.MONDAY), day("legs", DayOfWeek.SATURDAY)),
            program("other", isActive = false, day("arms", DayOfWeek.SATURDAY)),
        )
        val state = collectState(createViewModel())

        val success = state().success()
        assertEquals(LocalDate.of(2026, 10, 3), success.today)
        assertEquals(UpNextUi.Start("legs", "Legs", 5, null, scheduledOn = LocalDate.of(2026, 10, 3)), success.upNext)
        assertEquals(listOf("PPL", "OTHER"), success.programs.map { it.name })
        assertEquals(listOf(true, false), success.programs.map { it.isActive })
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), success.programs.first().trainingDays)
    }

    @Test
    fun uiState_onARestDay_upNextIsTheNextScheduledDay() = runTest {
        programs.programs.value = listOf(program("ppl", isActive = true, day("push", DayOfWeek.MONDAY)))
        val state = collectState(createViewModel())

        val upNext = state().success().upNext as UpNextUi.Start
        assertEquals("push", upNext.planId)
        assertEquals(LocalDate.of(2026, 10, 5), upNext.scheduledOn)
    }

    @Test
    fun uiState_activeProgramWithoutExercises_offersToOpenIt() = runTest {
        programs.programs.value = listOf(program("ppl", isActive = true, day("push", DayOfWeek.MONDAY, exercises = 0)))
        val state = collectState(createViewModel())

        assertEquals(UpNextUi.EmptyProgram("ppl", "PPL"), state().success().upNext)
    }

    @Test
    fun uiState_workoutInProgress_offersResume() = runTest {
        programs.programs.value = listOf(program("ppl", isActive = true, day("push", DayOfWeek.MONDAY)))
        workouts.activeWorkout.value = Workout(
            id = "w1", planId = "push", name = "Push Day",
            startedAt = Instant.parse("2026-10-03T08:30:00Z"), finishedAt = null, exercises = emptyList(),
        )
        val state = collectState(createViewModel())

        assertEquals(UpNextUi.Resume("w1", "Push Day", LocalTime.of(8, 30)), state().success().upNext)
    }

    @Test
    fun uiState_noPrograms_isEmptyState() = runTest {
        val state = collectState(createViewModel())

        val success = state().success()
        assertEquals(UpNextUi.NoPrograms, success.upNext)
        assertTrue(success.programs.isEmpty())
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
    fun onCreateProgram_namesDaysAfterTheSplitAndOpensTheProgram() = runTest {
        val viewModel = createViewModel()

        viewModel.onCreateProgram(
            ProgramSetup(
                name = "  PPL ",
                dayLabels = listOf("Push", "Pull", "Legs"),
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            ),
        )

        val (name, schedule) = programs.created.single()
        assertEquals("PPL", name)
        assertEquals(
            mapOf(
                DayOfWeek.MONDAY to "Push",
                DayOfWeek.WEDNESDAY to "Pull",
                DayOfWeek.FRIDAY to "Legs",
                DayOfWeek.SATURDAY to "Push",
            ),
            schedule,
        )
        assertEquals(HomeEvent.OpenProgram("program-1"), viewModel.events.first())
    }

    @Test
    fun onCreateProgram_customSplit_namesDaysAfterTheirWeekday() = runTest {
        createViewModel().onCreateProgram(ProgramSetup("Mine", emptyList(), setOf(DayOfWeek.TUESDAY)))

        assertEquals(mapOf(DayOfWeek.TUESDAY to "Tuesday"), programs.created.single().second)
    }

    @Test
    fun onCreateProgram_blankName_isIgnored() = runTest {
        createViewModel().onCreateProgram(ProgramSetup("   ", emptyList(), emptySet()))

        assertTrue(programs.created.isEmpty())
    }

    @Test
    fun onCreateProgram_failure_showsMessage() = runTest {
        programs.writeError = IllegalStateException("disk")
        val viewModel = createViewModel()

        viewModel.onCreateProgram(ProgramSetup("PPL", emptyList(), emptySet()))

        assertEquals(HomeEvent.ShowMessage(R.string.home_error_create_program), viewModel.events.first())
    }
}
