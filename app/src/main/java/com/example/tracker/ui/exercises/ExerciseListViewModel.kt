package com.example.tracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.repository.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Progress tab: the exercise library, each opening its Exercise details. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModel(private val exerciseRepository: ExerciseRepository) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    val uiState: StateFlow<ExerciseListUiState> = loadAttempt
        .flatMapLatest {
            exerciseRepository.observeExercises()
                .map { exercises ->
                    ExerciseListUiState.Success(
                        exercises
                            .sortedBy { it.name.lowercase() }
                            .map { ExerciseItemUi(it.id, it.name, it.type) },
                    ) as ExerciseListUiState
                }
                .onStart { emit(ExerciseListUiState.Loading) }
                .catch { emit(ExerciseListUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExerciseListUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ExerciseListViewModel(container.exerciseRepository)
            }
        }
    }
}
