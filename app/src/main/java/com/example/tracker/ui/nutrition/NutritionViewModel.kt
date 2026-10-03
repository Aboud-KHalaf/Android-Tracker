package com.example.tracker.ui.nutrition

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.nutrition.NutritionPeriod
import com.example.tracker.domain.repository.NutritionRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.ui.common.launchCatching
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Nutrition: today's calories and protein against the targets, the history of a period, and
 * the dialog that logs one day, and the daily targets. Messages are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(
    private val nutritionRepository: NutritionRepository,
    private val settingsRepository: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val mapper = NutritionStateMapper()
    private val loadAttempt = MutableStateFlow(0)
    private val period = MutableStateFlow<NutritionPeriod>(NutritionPeriod.ThisMonth)
    private val metric = MutableStateFlow(NutritionMetric.CALORIES)

    val uiState: StateFlow<NutritionUiState> = combine(loadAttempt, period) { _, period -> period }
        .flatMapLatest { period ->
            val today = today()
            combine(
                nutritionRepository.observeDays(period.start(today), period.end(today)),
                nutritionRepository.observeDay(today),
                settingsRepository.nutritionTargets,
                metric,
            ) { days, todayEntry, targets, metric ->
                mapper.map(today, todayEntry, period, metric, days, targets) as NutritionUiState
            }
                .onStart { emit(NutritionUiState.Loading) }
                .catch { emit(NutritionUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), NutritionUiState.Loading)

    /**
     * Whether to ask for notification permission now: once, on the first visit, while the
     * reminder is on. The screen checks whether the permission is actually missing.
     */
    val shouldRequestNotificationPermission: StateFlow<Boolean> =
        combine(settingsRepository.nutritionReminderEnabled, settingsRepository.notificationPermissionRequested) { enabled, requested ->
            enabled && !requested
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun onNotificationPermissionRequested() {
        viewModelScope.launchCatching(onError = {}) { settingsRepository.markNotificationPermissionRequested() }
    }

    private val _editor = MutableStateFlow<DayEditorState?>(null)

    /** The open "Log day" dialog, or null when it is closed. */
    val editor: StateFlow<DayEditorState?> = _editor.asStateFlow()

    private val _events = Channel<NutritionEvent>(Channel.BUFFERED)
    val events: Flow<NutritionEvent> = _events.receiveAsFlow()

    fun onSelectPeriod(selected: NutritionPeriod) {
        period.value = selected
    }

    fun onSelectMetric(selected: NutritionMetric) {
        metric.value = selected
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    /** Opens the dialog for [date], or today, filled in with what is already logged. */
    fun onOpenEditor(date: LocalDate? = null) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.nutrition_error_open_day) }) {
            val day = date ?: today()
            val existing = nutritionRepository.observeDay(day).first()
            _editor.value = DayEditorState(
                date = day,
                calories = existing?.calories?.toString().orEmpty(),
                protein = existing?.proteinGrams?.toString().orEmpty(),
                isExisting = existing != null,
            )
        }
    }

    /** Moves the dialog to [date]; a day that is already logged brings its amounts along. */
    fun onEditorDateChange(date: LocalDate) {
        if (_editor.value == null || date.isAfter(today())) return
        viewModelScope.launchCatching(onError = { showMessage(R.string.nutrition_error_open_day) }) {
            val existing = nutritionRepository.observeDay(date).first()
            _editor.update { editor ->
                when {
                    editor == null -> null
                    existing != null -> DayEditorState(date, existing.calories.toString(), existing.proteinGrams.toString(), isExisting = true)
                    else -> editor.copy(date = date, isExisting = false)
                }
            }
        }
    }

    fun onEditorCaloriesChange(text: String) {
        _editor.update { it?.copy(calories = sanitizeAmount(text), caloriesError = false) }
    }

    fun onEditorProteinChange(text: String) {
        _editor.update { it?.copy(protein = sanitizeAmount(text), proteinError = false) }
    }

    fun onDismissEditor() {
        _editor.value = null
    }

    fun onSaveDay() {
        val editor = _editor.value ?: return
        if (editor.isSaving) return
        val calories = parseAmount(editor.calories, DailyNutrition.MAX_CALORIES)
        val protein = parseAmount(editor.protein, DailyNutrition.MAX_PROTEIN_GRAMS)
        if (calories == null || protein == null) {
            _editor.value = editor.copy(caloriesError = calories == null, proteinError = protein == null)
            return
        }
        _editor.value = editor.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                _editor.update { it?.copy(isSaving = false) }
                showMessage(R.string.nutrition_error_save)
            },
        ) {
            nutritionRepository.saveDay(DailyNutrition(editor.date, calories, protein))
            _editor.value = null
        }
    }

    /** Deletes the day open in the dialog; the screen offers to undo. */
    fun onDeleteDay() {
        val editor = _editor.value?.takeIf { it.isExisting && !it.isSaving } ?: return
        viewModelScope.launchCatching(onError = { showMessage(R.string.nutrition_error_delete) }) {
            val deleted = nutritionRepository.observeDay(editor.date).first() ?: return@launchCatching
            nutritionRepository.deleteDay(editor.date)
            _editor.value = null
            _events.send(NutritionEvent.DayDeleted(deleted))
        }
    }

    fun onUndoDelete(day: DailyNutrition) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.nutrition_error_save) }) {
            nutritionRepository.saveDay(day)
        }
    }

    fun onSaveTargets(targets: NutritionTargets) {
        viewModelScope.launchCatching(onError = { showMessage(R.string.nutrition_error_targets) }) {
            settingsRepository.setNutritionTargets(targets)
        }
    }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(NutritionEvent.ShowMessage(messageRes))
    }

    private fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                NutritionViewModel(container.nutritionRepository, container.settingsRepository, container.time)
            }
        }
    }
}
