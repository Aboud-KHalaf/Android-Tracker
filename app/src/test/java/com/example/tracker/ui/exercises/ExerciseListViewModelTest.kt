package com.example.tracker.ui.exercises

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.domain.progress.toSessions
import com.example.tracker.domain.progress.weightSet
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository()
    private val workouts = FakeWorkoutRepository()

    // Wednesday 5 August 2026: this week is days 2–8 of the fixtures, last week days -5–1.
    private val time = FakeTimeProvider(Instant.parse("2026-08-05T12:00:00Z"))

    @Test
    fun uiState_listsExercisesByName() = runTest {
        exercises.exercises.value = listOf(
            Exercise("plank", "Plank", ExerciseType.DURATION),
            Exercise("bench", "bench press", ExerciseType.WEIGHT_REPS),
        )
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(
            ExerciseListUiState.Success(
                listOf(
                    ExerciseItemUi("bench", "bench press", ExerciseType.WEIGHT_REPS),
                    ExerciseItemUi("plank", "Plank", ExerciseType.DURATION),
                ),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun uiState_noExercises_isEmptySuccess() = runTest {
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(ExerciseListUiState.Success(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun muscleWeek_countsWorkingSetsPerGroupThisWeekAndLast() = runTest {
        exercises.exercises.value = listOf(
            Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS, MuscleGroup.CHEST),
            Exercise("curl", "Curl", ExerciseType.WEIGHT_REPS),
        )
        val sets = listOf(
            weightSet("now", 3, 60.0, 8), weightSet("now", 3, 60.0, 8), weightSet("now", 3, 60.0, 8),
            weightSet("before", 0, 60.0, 8), weightSet("before", 0, 60.0, 8),
            weightSet("now2", 4, 12.0, 10, exercise = "curl"),
            weightSet("long-ago", -20, 60.0, 8),
        )
        workouts.sessions.value = sets.toSessions().groupBy { it.exerciseId }
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val muscleWeek = (viewModel.uiState.value as ExerciseListUiState.Success).muscleWeek
        assertEquals(MuscleSetsUi(MuscleGroup.CHEST, thisWeek = 3, lastWeek = 2), muscleWeek.first { it.group == MuscleGroup.CHEST })
        assertEquals(MuscleSetsUi(null, thisWeek = 1, lastWeek = 0), muscleWeek.last())
        assertEquals(MuscleGroup.entries.size + 1, muscleWeek.size)
    }

    @Test
    fun muscleWeek_noRecentSets_isHidden() = runTest {
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS, MuscleGroup.CHEST))
        workouts.sessions.value = mapOf("bench" to listOf(weightSet("long-ago", -20, 60.0, 8)).toSessions())
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(emptyList<MuscleSetsUi>(), (viewModel.uiState.value as ExerciseListUiState.Success).muscleWeek)
    }

    @Test
    fun onQueryChange_keepsExercisesWhoseNameContainsIt_ignoringCase() = runTest {
        exercises.exercises.value = listOf(
            Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS, MuscleGroup.CHEST),
            Exercise("incline", "Incline Dumbbell Press", ExerciseType.WEIGHT_REPS, MuscleGroup.CHEST),
            Exercise("squat", "Squat", ExerciseType.WEIGHT_REPS, MuscleGroup.LEGS),
        )
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("  PRESS ")

        val state = viewModel.uiState.value as ExerciseListUiState.Success
        assertEquals(listOf("bench", "incline"), state.exercises.map { it.id })
        assertEquals("  PRESS ", viewModel.query.value)
        assertTrue(state.hasExercises)
    }

    @Test
    fun onMuscleFilterChange_keepsExercisesOfThatGroup_andCombinesWithTheSearch() = runTest {
        exercises.exercises.value = listOf(
            Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS, MuscleGroup.CHEST),
            Exercise("ohp", "Overhead Press", ExerciseType.WEIGHT_REPS, MuscleGroup.SHOULDERS),
            Exercise("fly", "Cable Fly", ExerciseType.WEIGHT_REPS, MuscleGroup.CHEST),
            Exercise("plank", "Plank", ExerciseType.DURATION),
        )
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onMuscleFilterChange(MuscleGroup.CHEST)
        var state = viewModel.uiState.value as ExerciseListUiState.Success
        assertEquals(listOf("bench", "fly"), state.exercises.map { it.id })
        assertEquals(MuscleGroup.CHEST, state.muscleFilter)
        // Only groups some exercise trains are offered, in their usual order.
        assertEquals(listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS), state.muscleOptions)

        viewModel.onQueryChange("press")
        state = viewModel.uiState.value as ExerciseListUiState.Success
        assertEquals(listOf("bench"), state.exercises.map { it.id })

        viewModel.onClearFilters()
        state = viewModel.uiState.value as ExerciseListUiState.Success
        assertEquals(4, state.exercises.size)
        assertNull(state.muscleFilter)
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun noMatches_isAnEmptyListOfALibraryThatHasExercises() = runTest {
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS))
        val viewModel = ExerciseListViewModel(exercises, workouts, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("deadlift")

        val state = viewModel.uiState.value as ExerciseListUiState.Success
        assertTrue(state.exercises.isEmpty())
        assertTrue(state.hasExercises)
    }
}
