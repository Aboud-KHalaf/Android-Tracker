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
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.launchCatching
import com.example.tracker.ui.program.ProgramSetup
import com.example.tracker.ui.program.schedule
import java.util.Locale
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
 * Home screen: the next workout from the active program, this week's training, programs and
 * recent workouts. State is exposed as [uiState]; navigation and messages are sent once
 * through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val time: TimeProvider,
    private val locale: () -> Locale = Locale::getDefault,
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
        viewModelScope.launchCatching(onError = { _events.send(HomeEvent.ShowMessage(R.string.home_error_start_workout)) }) {
            val workoutId = workoutRepository.startWorkout(planId)
            _events.send(HomeEvent.OpenWorkout(workoutId))
        }.invokeOnCompletion { isStartingWorkout.value = false }
    }

    /** Creates a program with one empty day per chosen weekday, named after the split's days. */
    fun onCreateProgram(setup: ProgramSetup) {
        val name = setup.name.trim()
        if (name.isEmpty()) return
        val schedule = setup.schedule(locale())
        viewModelScope.launchCatching(onError = { _events.send(HomeEvent.ShowMessage(R.string.home_error_create_program)) }) {
            val programId = programRepository.createProgram(name, schedule)
            _events.send(HomeEvent.OpenProgram(programId))
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
            programRepository.observePrograms(),
            workoutRepository.observeWeekSummary(weekStart),
            personalBestsWithNames,
            workoutRepository.observeHistory(),
        ) { active, programs, week, (bests, exercises), history ->
            mapper.map(today, HomeSources(active, programs, week, bests, exercises, history))
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                HomeViewModel(
                    programRepository = container.programRepository,
                    workoutRepository = container.workoutRepository,
                    exerciseRepository = container.exerciseRepository,
                    time = container.time,
                )
            }
        }
    }
}
