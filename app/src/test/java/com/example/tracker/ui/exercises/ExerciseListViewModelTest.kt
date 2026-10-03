package com.example.tracker.ui.exercises

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository()

    @Test
    fun uiState_listsExercisesByName() = runTest {
        exercises.exercises.value = listOf(
            Exercise("plank", "Plank", ExerciseType.DURATION),
            Exercise("bench", "bench press", ExerciseType.WEIGHT_REPS),
        )
        val viewModel = ExerciseListViewModel(exercises)
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
        val viewModel = ExerciseListViewModel(exercises)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(ExerciseListUiState.Success(emptyList()), viewModel.uiState.value)
    }
}
