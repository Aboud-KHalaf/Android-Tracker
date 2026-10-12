package com.example.tracker.ui.settings

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.domain.progress.benchHistory
import com.example.tracker.domain.progress.toSessions
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakeNutritionRepository
import com.example.tracker.testing.FakeReportExporter
import com.example.tracker.testing.FakeSettingsRepository
import com.example.tracker.testing.FakeWeightRepository
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val exercises = FakeExerciseRepository()
    private val workouts = FakeWorkoutRepository()
    private val nutrition = FakeNutritionRepository()
    private val weights = FakeWeightRepository()
    private val exporter = FakeReportExporter()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))

    private fun createViewModel() = SettingsViewModel(
        settingsRepository = settings,
        data = SettingsViewModel.ExportableData(exercises, workouts, nutrition, weights),
        reportExporter = exporter,
        time = time,
    )

    @Test
    fun uiState_showsStoredThemeMode() = runTest {
        settings.themeMode.value = ThemeMode.LIGHT
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT), viewModel.uiState.value)
    }

    @Test
    fun onSelectThemeMode_savesIt() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSelectThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settings.themeMode.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun onSelectThemeMode_failure_showsError() = runTest {
        settings.writeError = IllegalStateException("disk full")
        val viewModel = createViewModel()

        viewModel.onSelectThemeMode(ThemeMode.DARK)

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_theme), viewModel.events.first())
        assertEquals(ThemeMode.SYSTEM, settings.themeMode.value)
    }

    @Test
    fun onDeleteAllData_deletesAndConfirms() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onDeleteAllData()

        assertEquals(1, settings.deleteAllDataCalls)
        assertEquals(SettingsEvent.ShowMessage(R.string.settings_data_deleted), viewModel.events.first())
        assertFalse(viewModel.uiState.value.isDeletingData)
    }

    @Test
    fun onDeleteAllData_failure_showsErrorAndAllowsRetry() = runTest {
        settings.writeError = IllegalStateException("db locked")
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onDeleteAllData()

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_delete_data), viewModel.events.first())
        assertFalse(viewModel.uiState.value.isDeletingData)
    }

    @Test
    fun onSetNutritionReminder_savesIt() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetNutritionReminder(false)

        assertFalse(settings.nutritionReminderEnabled.value)
        assertFalse(viewModel.uiState.value.nutritionReminderEnabled)
    }

    @Test
    fun onSetNutritionReminder_failure_showsMessage() = runTest {
        settings.writeError = java.io.IOException("disk")
        val viewModel = createViewModel()

        viewModel.onSetNutritionReminder(false)

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_reminder), viewModel.events.first())
    }

    @Test
    fun onSetNutritionReminderTime_savesIt() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetNutritionReminderTime(LocalTime.of(19, 15))

        assertEquals(LocalTime.of(19, 15), settings.nutritionReminderTime.value)
        assertEquals(LocalTime.of(19, 15), viewModel.uiState.value.nutritionReminderTime)
    }

    @Test
    fun onSetNutritionReminderTime_failure_showsMessage() = runTest {
        settings.writeError = java.io.IOException("disk")
        val viewModel = createViewModel()

        viewModel.onSetNutritionReminderTime(LocalTime.of(19, 15))

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_reminder), viewModel.events.first())
    }

    @Test
    fun onExportAllData_writesEverySectionsFullHistory() = runTest {
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS))
        workouts.sessions.value = mapOf("bench" to benchHistory.toSessions())
        nutrition.put(DailyNutrition(LocalDate.of(2024, 1, 5), calories = 2000, proteinGrams = 150))
        weights.put(WeightEntry(LocalDate.of(2023, 6, 1), 82.0), WeightEntry(LocalDate.of(2026, 10, 3), 78.0))
        val viewModel = createViewModel()

        viewModel.onExportAllData(ExportFormat.XLSX, "content://all.xlsx")

        assertEquals(SettingsEvent.ShowMessage(R.string.export_done), viewModel.events.first())
        val sections = exporter.exports.single().report.sections
        assertEquals(
            listOf(
                R.string.export_section_exercises,
                R.string.export_section_sets,
                R.string.nutrition_title,
                R.string.export_section_days,
                R.string.weight_title,
                R.string.export_section_entries,
            ),
            sections.map { (it.title as ReportText.Res).id },
        )
        assertEquals(18, sections[1].table!!.rows.size) // six sessions of three sets
        assertEquals(1, sections[3].table!!.rows.size)
        assertEquals(2, sections[5].table!!.rows.size)
        assertFalse(viewModel.uiState.value.isExportingData)
    }

    @Test
    fun onExportAllData_failure_showsError() = runTest {
        exporter.error = IOException("disk full")
        val viewModel = createViewModel()

        viewModel.onExportAllData(ExportFormat.PDF, "content://all.pdf")

        assertEquals(SettingsEvent.ShowMessage(R.string.export_error), viewModel.events.first())
    }

    @Test
    fun exportFileName_isDated() {
        assertEquals("tracker-all-data-2026-10-03.pdf", createViewModel().exportFileName(ExportFormat.PDF))
    }
}
