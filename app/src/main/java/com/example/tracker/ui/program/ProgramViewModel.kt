package com.example.tracker.ui.program

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.ui.common.launchCatching
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Program screen: the weekly schedule and the training days of one program.
 * State is exposed as [uiState]; navigation and messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModel(
    private val programId: String,
    private val programRepository: ProgramRepository,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)

    private val _events = Channel<ProgramEvent>(Channel.BUFFERED)
    val events: Flow<ProgramEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ProgramUiState> = loadAttempt
        .flatMapLatest {
            programRepository.observeProgram(programId)
                .map { it?.toUiState() ?: ProgramUiState.NotFound }
                .onStart { emit(ProgramUiState.Loading) }
                .catch { emit(ProgramUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgramUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /** Adds a training day on [dayOfWeek], or one without a fixed weekday when null. */
    fun onAddDay(dayOfWeek: DayOfWeek?, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { programRepository.addDay(programId, dayOfWeek, trimmed) }
    }

    fun onRemoveDay(dayId: String) = update { planRepository.deletePlan(dayId) }

    fun onScheduleDay(dayId: String, dayOfWeek: DayOfWeek?) = update { programRepository.scheduleDay(dayId, dayOfWeek) }

    fun onMakeActive() = update { programRepository.setActiveProgram(programId) }

    fun onRenameProgram(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { programRepository.renameProgram(programId, trimmed) }
    }

    fun onDeleteProgram() {
        viewModelScope.launchCatching(onError = { showMessage(R.string.program_error_delete) }) {
            programRepository.deleteProgram(programId)
            _events.send(ProgramEvent.ProgramDeleted)
        }
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.program_error_update) }) { block() }
    }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(ProgramEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ProgramViewModel(
                    programId = createSavedStateHandle().toRoute<ProgramDestination>().programId,
                    programRepository = container.programRepository,
                    planRepository = container.planRepository,
                )
            }
        }
    }
}

internal fun Program.toUiState() = ProgramUiState.Success(
    name = name,
    isActive = isActive,
    days = days.map { ProgramDayUi(it.id, it.name, it.dayOfWeek, it.exerciseCount) },
)
