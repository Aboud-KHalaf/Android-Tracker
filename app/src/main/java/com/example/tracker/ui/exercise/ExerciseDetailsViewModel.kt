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
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportExporter
import com.example.tracker.domain.export.exportFileName
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.domain.progress.BodyweightLog
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.domain.progress.bodyweightStat
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.WorkoutRepository
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val reportExporter: ReportExporter,
    private val time: TimeProvider,
    private val statRepository: CustomStatRepository,
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
                observeBodyweight(),
            ) { exercise, sessions, metric, range, bodyweight ->
                mapper.map(exercise, sessions, metric, range, time.now(), bodyweight)
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

    /** Sets the body part the exercise trains, for the weekly sets per muscle; null clears it. */
    fun onSetMuscleGroup(group: MuscleGroup?) {
        viewModelScope.launchCatching(
            onError = { _events.send(ExerciseDetailsEvent.ShowMessage(R.string.exercise_error_muscle_group)) },
        ) {
            exerciseRepository.setMuscleGroup(exerciseId, group)
        }
    }

    private val _isExporting = MutableStateFlow(false)

    /** Whether an export is being written; the screen disables exporting meanwhile. */
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportFileName(format: ExportFormat): String {
        val name = (uiState.value as? ExerciseDetailsUiState.Success)?.name ?: "exercise"
        return exportFileName(name, time.now().atZone(time.zone()).toLocalDate(), format)
    }

    /** Saves the progress for the selected metric and range as [format] to [destination], a file URI. */
    fun onExport(format: ExportFormat, destination: String) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launchCatching(onError = { _events.send(ExerciseDetailsEvent.ShowMessage(R.string.export_error)) }) {
            try {
                val exercise = checkNotNull(exerciseRepository.observeExercise(exerciseId).first()) { "Exercise $exerciseId is gone" }
                val sessions = workoutRepository.observeExerciseSessions(exerciseId).first()
                val report = ExerciseReportBuilder(time.zone())
                    .build(exercise, sessions, metric.value, range.value, time.now(), observeBodyweight().first())
                reportExporter.export(report, format, destination)
                _events.send(ExerciseDetailsEvent.ShowMessage(R.string.export_done))
            } finally {
                _isExporting.value = false
            }
        }
    }

    /** The user's bodyweight log, if they track bodyweight; it unlocks relative strength. */
    private fun observeBodyweight(): Flow<BodyweightLog?> {
        val today = time.now().atZone(time.zone()).toLocalDate()
        return statRepository.observeOverviews()
            .map { overviews -> overviews.map { it.stat }.bodyweightStat()?.id }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(null) else statRepository.observeEntries(id, HISTORY_START, today).map(::BodyweightLog)
            }
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        val DEFAULT_RANGE = TimeRange.THREE_MONTHS

        /** Before anything could have been logged, so "from here to today" is all history. */
        private val HISTORY_START: LocalDate = LocalDate.of(1900, 1, 1)

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ExerciseDetailsViewModel(
                    exerciseId = createSavedStateHandle().toRoute<ExerciseDetailsDestination>().exerciseId,
                    exerciseRepository = container.exerciseRepository,
                    workoutRepository = container.workoutRepository,
                    reportExporter = container.reportExporter,
                    time = container.time,
                    statRepository = container.customStatRepository,
                )
            }
        }
    }
}
