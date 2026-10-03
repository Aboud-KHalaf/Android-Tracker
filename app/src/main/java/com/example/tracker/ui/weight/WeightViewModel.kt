package com.example.tracker.ui.weight

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.domain.repository.WeightRepository
import com.example.tracker.ui.common.launchCatching
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Weight: the latest weight, how it moved over a period, and the dialog that logs one day.
 * Messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeightViewModel(
    private val weightRepository: WeightRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val mapper = WeightStateMapper()
    private val loadAttempt = MutableStateFlow(0)
    private val period = MutableStateFlow<DatePeriod>(DatePeriod.Last30Days)

    val uiState: StateFlow<WeightUiState> = combine(loadAttempt, period) { _, period -> period }
        .flatMapLatest { period ->
            val today = today()
            combine(
                weightRepository.observeEntries(period.start(today), period.end(today)),
                weightRepository.observeLatest(),
            ) { entries, latest ->
                mapper.map(today, latest, period, entries) as WeightUiState
            }
                .onStart { emit(WeightUiState.Loading) }
                .catch { emit(WeightUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WeightUiState.Loading)

    private val _editor = MutableStateFlow<WeightEditorState?>(null)

    /** The open "Log weight" dialog, or null when it is closed. */
    val editor: StateFlow<WeightEditorState?> = _editor.asStateFlow()

    private val _events = Channel<WeightEvent>(Channel.BUFFERED)
    val events: Flow<WeightEvent> = _events.receiveAsFlow()

    fun onSelectPeriod(selected: DatePeriod) {
        period.value = selected
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /**
     * Opens the dialog for [date], or today. A logged day shows its weight; a new one starts
     * from the latest weight, which is usually close.
     */
    fun onOpenEditor(date: LocalDate? = null) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.weight_error_open) }) {
            val day = date ?: today()
            val existing = weightRepository.observeEntry(day).first()
            val start = existing ?: weightRepository.observeLatest().first()
            _editor.value = WeightEditorState(day, start?.weightKg?.toWeightInput().orEmpty(), isExisting = existing != null)
        }
    }

    /** Moves the dialog to [date]; a day that is already logged brings its weight along. */
    fun onEditorDateChange(date: LocalDate) {
        if (_editor.value == null || date.isAfter(today())) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.weight_error_open) }) {
            val existing = weightRepository.observeEntry(date).first()
            _editor.update { editor ->
                when {
                    editor == null -> null
                    existing != null -> WeightEditorState(date, existing.weightKg.toWeightInput(), isExisting = true)
                    else -> editor.copy(date = date, isExisting = false)
                }
            }
        }
    }

    fun onEditorWeightChange(text: String) {
        _editor.update { it?.copy(weight = sanitizeWeight(text), weightError = false) }
    }

    fun onDismissEditor() {
        _editor.value = null
    }

    fun onSaveEntry() {
        val editor = _editor.value ?: return
        if (editor.isSaving) return
        val weight = parseWeight(editor.weight)
        if (weight == null) {
            _editor.value = editor.copy(weightError = true)
            return
        }
        _editor.value = editor.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                _editor.update { it?.copy(isSaving = false) }
                showMessage(R.string.weight_error_save)
            },
        ) {
            // One decimal is all a bathroom scale shows; it also hides float noise like 78.39999.
            weightRepository.saveEntry(WeightEntry(editor.date, (weight * 10).roundToInt() / 10.0))
            _editor.value = null
        }
    }

    /** Deletes the entry open in the dialog; the screen offers to undo. */
    fun onDeleteEntry() {
        val editor = _editor.value?.takeIf { it.isExisting && !it.isSaving } ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.weight_error_delete) }) {
            val deleted = weightRepository.observeEntry(editor.date).first() ?: return@launchCatching
            weightRepository.deleteEntry(editor.date)
            _editor.value = null
            _events.send(WeightEvent.EntryDeleted(deleted))
        }
    }

    fun onUndoDelete(entry: WeightEntry) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.weight_error_save) }) {
            weightRepository.saveEntry(entry)
        }
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(WeightEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                WeightViewModel(container.weightRepository, container.time)
            }
        }
    }
}
