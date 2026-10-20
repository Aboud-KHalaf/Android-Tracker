package com.example.tracker.ui.stats.detail

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
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportExporter
import com.example.tracker.domain.export.exportFileName
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatOverview
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.ui.common.launchCatching
import com.example.tracker.ui.stats.parseStatInput
import com.example.tracker.ui.stats.roundTo
import com.example.tracker.ui.stats.sanitizeStatInput
import com.example.tracker.ui.stats.toStatInput
import java.time.LocalDate
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * One custom stat: its latest value and progress, how it moved over a period, and the dialog
 * that logs one day. With [logToday] the dialog opens for today right away.
 * Messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatDetailViewModel(
    private val statId: String,
    logToday: Boolean,
    private val repository: CustomStatRepository,
    private val reportExporter: ReportExporter,
    private val time: TimeProvider,
) : ViewModel() {

    private val mapper = StatDetailStateMapper()
    private val loadAttempt = MutableStateFlow(0)
    private val period = MutableStateFlow<DatePeriod>(DatePeriod.Last30Days)

    private val overview: Flow<StatOverview?> =
        repository.observeOverviews().map { overviews -> overviews.firstOrNull { it.stat.id == statId } }

    val uiState: StateFlow<StatDetailUiState> = combine(loadAttempt, period) { _, period -> period }
        .flatMapLatest { period ->
            val today = today()
            combine(
                overview,
                repository.observeEntries(statId, period.start(today), period.end(today)),
            ) { overview, entries ->
                if (overview == null) StatDetailUiState.Missing else mapper.map(today, overview, period, entries)
            }
                .onStart { emit(StatDetailUiState.Loading) }
                .catch { emit(StatDetailUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatDetailUiState.Loading)

    private val _editor = MutableStateFlow<StatEntryEditorState?>(null)

    /** The open "Log" dialog, or null when it is closed. */
    val editor: StateFlow<StatEntryEditorState?> = _editor.asStateFlow()

    private val _events = Channel<StatDetailEvent>(Channel.BUFFERED)
    val events: Flow<StatDetailEvent> = _events.receiveAsFlow()

    init {
        if (logToday) onOpenEditor()
    }

    fun onSelectPeriod(selected: DatePeriod) {
        period.value = selected
    }

    private val _isExporting = MutableStateFlow(false)

    /** Whether an export is being written; the screen disables exporting meanwhile. */
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportFileName(format: ExportFormat): String =
        exportFileName((uiState.value as? StatDetailUiState.Success)?.stat?.name.orEmpty(), today(), format)

    /** Saves the selected period's entries as [format] to [destination], a file URI. */
    fun onExport(format: ExportFormat, destination: String) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launchCatching(onError = { showMessage(R.string.export_error) }) {
            try {
                val stat = overview.first()?.stat ?: return@launchCatching
                val today = today()
                val period = period.value
                val from = period.start(today)
                val to = period.end(today)
                val entries = repository.observeEntries(statId, from, to).first()
                reportExporter.export(StatReportBuilder.build(stat, from, to, today, entries), format, destination)
                showMessage(R.string.export_done)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /**
     * Opens the dialog for [date], or today. A logged day shows its value; a new one starts
     * from the latest value, which is usually close.
     */
    fun onOpenEditor(date: LocalDate? = null) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.stat_error_open) }) {
            val day = date ?: today()
            val overview = overview.first() ?: return@launchCatching
            val existing = repository.observeEntry(statId, day).first()
            val start = existing ?: overview.latest
            _editor.value = StatEntryEditorState(
                date = day,
                value = start?.value?.toStatInput(overview.stat.decimals).orEmpty(),
                decimals = overview.stat.decimals,
                isExisting = existing != null,
            )
        }
    }

    /** Moves the dialog to [date]; a day that is already logged brings its value along. */
    fun onEditorDateChange(date: LocalDate) {
        if (_editor.value == null || date.isAfter(today())) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.stat_error_open) }) {
            val existing = repository.observeEntry(statId, date).first()
            _editor.update { editor ->
                when {
                    editor == null -> null
                    existing != null -> editor.copy(
                        date = date,
                        value = existing.value.toStatInput(editor.decimals),
                        isExisting = true,
                        valueError = false,
                    )
                    else -> editor.copy(date = date, isExisting = false)
                }
            }
        }
    }

    fun onEditorValueChange(text: String) {
        _editor.update { it?.copy(value = sanitizeStatInput(text, it.decimals), valueError = false) }
    }

    fun onDismissEditor() {
        _editor.value = null
    }

    fun onSaveEntry() {
        val editor = _editor.value ?: return
        if (editor.isSaving) return
        val value = parseStatInput(editor.value)
        if (value == null) {
            _editor.value = editor.copy(valueError = true)
            return
        }
        _editor.value = editor.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                _editor.update { it?.copy(isSaving = false) }
                showMessage(R.string.stat_error_save)
            },
        ) {
            repository.saveEntry(StatEntry(statId, editor.date, roundTo(value, editor.decimals)))
            _editor.value = null
        }
    }

    /** Deletes the entry open in the dialog; the screen offers to undo. */
    fun onDeleteEntry() {
        val editor = _editor.value?.takeIf { it.isExisting && !it.isSaving } ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.stat_error_delete) }) {
            val deleted = repository.observeEntry(statId, editor.date).first() ?: return@launchCatching
            repository.deleteEntry(statId, editor.date)
            _editor.value = null
            _events.send(StatDetailEvent.EntryDeleted(deleted))
        }
    }

    fun onUndoDelete(entry: StatEntry) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.stat_error_save) }) {
            repository.saveEntry(entry)
        }
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(StatDetailEvent.ShowMessage(messageRes))
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                val route = createSavedStateHandle().toRoute<StatDetailDestination>()
                StatDetailViewModel(
                    route.statId,
                    route.logToday,
                    container.customStatRepository,
                    container.reportExporter,
                    container.time,
                )
            }
        }
    }
}
