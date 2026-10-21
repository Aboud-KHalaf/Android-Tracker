package com.example.tracker.ui.settings

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.progress.benchHistory
import com.example.tracker.domain.progress.toSessions
import com.example.tracker.domain.workout.RestTimes
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakeReportExporter
import com.example.tracker.testing.FakeSettingsRepository
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
    private val stats = FakeCustomStatRepository()
    private val exporter = FakeReportExporter()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))

    private fun createViewModel() = SettingsViewModel(
        settingsRepository = settings,
        data = SettingsViewModel.ExportableData(exercises, workouts, stats),
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
    fun onSetReminder_savesIt() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetReminder(false)

        assertFalse(settings.reminderEnabled.value)
        assertFalse(viewModel.uiState.value.reminderEnabled)
    }

    @Test
    fun onSetReminder_failure_showsMessage() = runTest {
        settings.writeError = java.io.IOException("disk")
        val viewModel = createViewModel()

        viewModel.onSetReminder(false)

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_reminder), viewModel.events.first())
    }

    @Test
    fun onSetReminderTime_savesIt() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetReminderTime(LocalTime.of(19, 15))

        assertEquals(LocalTime.of(19, 15), settings.reminderTime.value)
        assertEquals(LocalTime.of(19, 15), viewModel.uiState.value.reminderTime)
    }

    @Test
    fun onSetReminderTime_failure_showsMessage() = runTest {
        settings.writeError = java.io.IOException("disk")
        val viewModel = createViewModel()

        viewModel.onSetReminderTime(LocalTime.of(19, 15))

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_reminder), viewModel.events.first())
    }

    @Test
    fun onSetRestTimes_savesEachOne() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetRestBetweenSets(60)
        viewModel.onSetRestBetweenExercises(0)

        assertEquals(RestTimes(betweenSetsSeconds = 60, betweenExercisesSeconds = 0), settings.restTimes.value)
        assertEquals(settings.restTimes.value, viewModel.uiState.value.restTimes)
    }

    @Test
    fun onSetRestTime_failure_showsMessage() = runTest {
        settings.writeError = IllegalStateException("disk full")
        val viewModel = createViewModel()

        viewModel.onSetRestBetweenSets(60)

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_rest), viewModel.events.first())
    }

    @Test
    fun onExportAllData_writesEverySectionsFullHistory() = runTest {
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS))
        workouts.sessions.value = mapOf("bench" to benchHistory.toSessions())
        stats.stats.value = listOf(
            CustomStat("calories", "Calories", "kcal", 0, 2300.0, StatIcon.FIRE, StatColor.AMBER),
            CustomStat("weight", "Weight", "kg", 1, null, StatIcon.SCALE, StatColor.BLUE),
        )
        stats.put(
            StatEntry("calories", LocalDate.of(2024, 1, 5), 2000.0),
            StatEntry("weight", LocalDate.of(2023, 6, 1), 82.0),
            StatEntry("weight", LocalDate.of(2026, 10, 3), 78.0),
        )
        val viewModel = createViewModel()

        viewModel.onExportAllData(ExportFormat.XLSX, "content://all.xlsx")

        assertEquals(SettingsEvent.ShowMessage(R.string.export_done), viewModel.events.first())
        val sections = exporter.exports.single().report.sections
        assertEquals(
            listOf("exercises", "sets", "Calories", "Calories entries", "Weight", "Weight entries"),
            sections.map { title ->
                when (val text = title.title) {
                    is ReportText.Plain -> text.text
                    is ReportText.Res -> when (text.id) {
                        R.string.export_section_exercises -> "exercises"
                        R.string.export_section_sets -> "sets"
                        R.string.export_stat_entries -> "${text.args.single()} entries"
                        else -> error("Unexpected section ${text.id}")
                    }
                }
            },
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

    @Test
    fun onSetDynamicColor_savesItAndShowsIt() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetDynamicColor(true)

        assertEquals(true, settings.dynamicColor.value)
        assertEquals(true, viewModel.uiState.value.dynamicColor)
    }
}
