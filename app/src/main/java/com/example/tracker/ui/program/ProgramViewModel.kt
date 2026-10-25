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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Program screen: the weekly schedule and the training days of one program, in the user's order.
 * State is exposed as [uiState]; navigation and messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModel(
    private val programId: String,
    private val programRepository: ProgramRepository,
    private val planRepository: PlanRepository,
) : ViewModel() {

    private val loadAttempt = MutableStateFlow(0)
    private val removalToConfirm = MutableStateFlow<String?>(null)

    /**
     * Weekdays whose toggle was saved but isn't in [uiState] yet, with whether they will be
     * training days. Taps on them are ignored, so a double tap doesn't toggle twice.
     */
    private val pendingWeekdays = mutableMapOf<DayOfWeek, Boolean>()

    private val _events = Channel<ProgramEvent>(Channel.BUFFERED)
    val events: Flow<ProgramEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ProgramUiState> = loadAttempt
        .flatMapLatest {
            combine(programRepository.observeProgram(programId), removalToConfirm) { program, removalId ->
                program?.toUiState(removalId) ?: ProgramUiState.NotFound
            }
                .onEach { state ->
                    val days = (state as? ProgramUiState.Success)?.trainingDays ?: return@onEach
                    pendingWeekdays.entries.removeAll { (weekday, scheduled) -> (weekday in days) == scheduled }
                }
                .onStart { emit(ProgramUiState.Loading) }
                .catch { emit(ProgramUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgramUiState.Loading)

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /**
     * A weekday chip was tapped: adds a training day named [name] there, or removes the day on
     * it. A day with exercises is only removed after the user confirms.
     */
    fun onToggleWeekday(weekday: DayOfWeek, name: String) {
        val state = uiState.value as? ProgramUiState.Success ?: return
        if (weekday in pendingWeekdays) return
        val existing = state.days.firstOrNull { it.dayOfWeek == weekday }
        when {
            existing == null -> toggle(weekday, scheduled = true) { programRepository.addDay(programId, weekday, name) }
            existing.exerciseCount > 0 -> removalToConfirm.value = existing.id
            else -> toggle(weekday, scheduled = false) { planRepository.deletePlan(existing.id) }
        }
    }

    /** Asks to confirm removing a day, e.g. from its menu. */
    fun onRequestRemoveDay(dayId: String) {
        removalToConfirm.value = dayId
    }

    fun onConfirmRemoveDay() {
        val dayId = removalToConfirm.value ?: return
        removalToConfirm.value = null
        update { planRepository.deletePlan(dayId) }
    }

    fun onCancelRemoveDay() {
        removalToConfirm.value = null
    }

    /** Adds a training day on [dayOfWeek], or one without a fixed weekday when null. */
    fun onAddDay(dayOfWeek: DayOfWeek?, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { programRepository.addDay(programId, dayOfWeek, trimmed) }
    }

    fun onScheduleDay(dayId: String, dayOfWeek: DayOfWeek?) = update { programRepository.scheduleDay(dayId, dayOfWeek) }

    /** Saves the order the user arranged the days in, listing every day once. */
    fun onReorderDays(dayIds: List<String>) = update { programRepository.reorderDays(programId, dayIds) }

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

    private fun toggle(weekday: DayOfWeek, scheduled: Boolean, block: suspend () -> Unit) {
        pendingWeekdays[weekday] = scheduled
        viewModelScope.launchCatching(
            onError = {
                pendingWeekdays.remove(weekday)
                showMessage(R.string.program_error_update)
            },
        ) { block() }
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

internal fun Program.toUiState(removalToConfirmId: String? = null): ProgramUiState.Success {
    val dayUis = days.map { ProgramDayUi(it.id, it.name, it.dayOfWeek, it.exerciseCount) }
    return ProgramUiState.Success(
        name = name,
        isActive = isActive,
        days = dayUis,
        // A day deleted meanwhile has nothing left to confirm.
        removalToConfirm = dayUis.firstOrNull { it.id == removalToConfirmId },
    )
}
