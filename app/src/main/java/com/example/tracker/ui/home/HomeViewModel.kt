package com.example.tracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.launch

/**
 * Home screen: the suggested workout, this week's training, plans and recent workouts.
 * State is exposed as [uiState]; navigation and messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val mapper = HomeStateMapper(time.zone())
    private val loadAttempt = MutableStateFlow(0)
    private val isStartingWorkout = MutableStateFlow(false)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = loadAttempt
        .flatMapLatest {
            combine(observeContent(), isStartingWorkout) { content, starting ->
                content.copy(isStartingWorkout = starting) as HomeUiState
            }
                .onStart { emit(HomeUiState.Loading) }
                .catch { emit(HomeUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    fun onStartWorkout(planId: String) {
        if (isStartingWorkout.value) return
        isStartingWorkout.value = true
        viewModelScope.launch {
            try {
                val workoutId = workoutRepository.startWorkout(planId)
                _events.send(HomeEvent.OpenWorkout(workoutId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowMessage(R.string.home_error_start_workout))
            } finally {
                isStartingWorkout.value = false
            }
        }
    }

    fun onCreatePlan(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val planId = planRepository.createPlan(trimmed)
                _events.send(HomeEvent.OpenPlan(planId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowMessage(R.string.home_error_create_plan))
            }
        }
    }

    private fun observeContent(): Flow<HomeUiState.Success> {
        val today = time.now().atZone(time.zone()).toLocalDate()
        val weekStart = HomeStateMapper.startOfWeek(today)
        val personalBestsWithNames = combine(
            workoutRepository.observePersonalBests(),
            exerciseRepository.observeExercises(),
        ) { bests, exercises -> bests to exercises }

        return combine(
            workoutRepository.observeActiveWorkout(),
            planRepository.observePlans(),
            workoutRepository.observeWeekSummary(weekStart),
            personalBestsWithNames,
            workoutRepository.observeHistory(),
        ) { active, plans, week, (bests, exercises), history ->
            mapper.map(today, active, plans, week, bests, exercises, history)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                HomeViewModel(
                    planRepository = container.planRepository,
                    workoutRepository = container.workoutRepository,
                    exerciseRepository = container.exerciseRepository,
                    time = container.time,
                )
            }
        }
    }
}
