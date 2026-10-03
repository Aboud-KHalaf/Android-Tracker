package com.example.tracker.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.launchCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Exercise details: progress over time by a chosen metric and range, and every session.
 * State is exposed as [uiState]; messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailsViewModel(
    private val exerciseId: String,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val mapper = ExerciseDetailsStateMapper(time.zone())
    private val loadAttempt = MutableStateFlow(0)

    /** Null until the user picks one: then the exercise type's default applies. */
    private val metric = MutableStateFlow<ProgressMetric?>(null)
    private val range = MutableStateFlow(DEFAULT_RANGE)

    private val _events = Channel<ExerciseDetailsEvent>(Channel.BUFFERED)
    val events: Flow<ExerciseDetailsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ExerciseDetailsUiState> = loadAttempt
        .flatMapLatest {
            combine(
                exerciseRepository.observeExercise(exerciseId),
                workoutRepository.observeExerciseSessions(exerciseId),
                metric,
                range,
            ) { exercise, sessions, metric, range ->
                mapper.map(exercise, sessions, metric, range, time.now())
            }
                .onStart { emit(ExerciseDetailsUiState.Loading) }
                .catch { emit(ExerciseDetailsUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExerciseDetailsUiState.Loading)

    fun onSelectMetric(selected: ProgressMetric) {
        metric.value = selected
    }

    fun onSelectRange(selected: TimeRange) {
        range.value = selected
    }

    fun onRenameExercise(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launchCatching(
            onError = { _events.send(ExerciseDetailsEvent.ShowMessage(R.string.exercise_error_rename)) },
        ) {
            exerciseRepository.renameExercise(exerciseId, trimmed)
        }
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        val DEFAULT_RANGE = TimeRange.THREE_MONTHS

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ExerciseDetailsViewModel(
                    exerciseId = createSavedStateHandle().toRoute<ExerciseDetailsDestination>().exerciseId,
                    exerciseRepository = container.exerciseRepository,
                    workoutRepository = container.workoutRepository,
                    time = container.time,
                )
            }
        }
    }
}
