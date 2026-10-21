package com.example.tracker.ui.exercise

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.textCell
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.domain.progress.benchHistory
import com.example.tracker.domain.progress.holdSet
import com.example.tracker.domain.progress.toSessions
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakeReportExporter
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.common.SetValueUi
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository()
    private val workouts = FakeWorkoutRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val exporter = FakeReportExporter()
    private val stats = FakeCustomStatRepository()

    @Before
    fun setUp() {
        // Six Bench Press sessions, Aug 1 to Sep 5; top sets 40 → 50 kg.
        exercises.exercises.value = listOf(Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS))
        workouts.sessions.value = mapOf("bench" to benchHistory.toSessions())
    }

    private fun createViewModel(exerciseId: String = "bench") =
        ExerciseDetailsViewModel(exerciseId, exercises, workouts, exporter, time, stats)

    private fun TestScope.collectState(viewModel: ExerciseDetailsViewModel): () -> ExerciseDetailsUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun ExerciseDetailsUiState.success() = this as ExerciseDetailsUiState.Success

    @Test
    fun uiState_defaultsToWeightOverThreeMonths() = runTest {
        val state = collectState(createViewModel())().success()

        assertEquals("Bench Press", state.name)
        assertEquals(listOf(ProgressMetric.WEIGHT, ProgressMetric.REPS, ProgressMetric.VOLUME), state.metrics)
        assertEquals(ProgressMetric.WEIGHT, state.metric)
        assertEquals(TimeRange.THREE_MONTHS, state.range)

        val progress = state.progress!!
        assertEquals(listOf(40.0, 40.0, 42.5, 42.5, 45.0, 50.0), progress.chart.points.map { it.value })
        assertEquals(10.0, progress.change, 0.0)
        assertEquals(Trend.UP, progress.trend)
        assertEquals(LocalDate.of(2026, 8, 1), progress.since)
    }

    @Test
    fun uiState_summarizesBestAndSessionsNewestFirst() = runTest {
        val state = collectState(createViewModel())().success()

        assertEquals(PersonalBestSummaryUi(SetValueUi.WeightReps(50.0, 8), LocalDate.of(2026, 9, 5)), state.personalBest)
        assertEquals(6, state.sessionCount)
        assertEquals(LocalDate.of(2026, 8, 1), state.firstSessionOn)
        assertEquals(listOf("w6", "w5", "w4", "w3", "w2", "w1"), state.sessions.map { it.workoutId })
        assertEquals(listOf(true, true, false, true, false, false), state.sessions.map { it.isPersonalBest })
        assertEquals(SetValueUi.WeightReps(50.0, 8), state.sessions.first().best)
        assertEquals(3, state.sessions.first().sets.size)
        assertEquals(listOf("w6", "w5", "w4"), state.recentSessions.map { it.workoutId })
    }

    @Test
    fun onSelectMetricAndRange_rechartsInRange() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSelectMetric(ProgressMetric.VOLUME)
        assertEquals(1_250.0, state().success().progress!!.chart.last.value, 0.0) // 45×10 + 50×8 + 50×8

        viewModel.onSelectRange(TimeRange.ONE_MONTH)
        val progress = state().success().progress!!
        assertEquals(1, progress.chart.points.size)
        assertEquals(Trend.FLAT, progress.trend)
        // The range only affects the chart; history stays complete.
        assertEquals(6, state().success().sessions.size)
    }

    @Test
    fun uiState_noSessionsInRange_hasNoProgress() = runTest {
        time.current = Instant.parse("2027-06-01T09:00:00Z")
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSelectRange(TimeRange.ONE_MONTH)

        assertNull(state().success().progress)
        assertEquals(6, state().success().sessionCount)
    }

    @Test
    fun uiState_holdExercise_chartsLongestHold() = runTest {
        exercises.exercises.value += Exercise("plank", "Plank", ExerciseType.DURATION)
        workouts.sessions.value += "plank" to listOf(
            holdSet("p1", 0, 40), holdSet("p1", 0, 45), holdSet("p2", 7, 50),
        ).toSessions()

        val state = collectState(createViewModel("plank"))().success()

        assertEquals(listOf(ProgressMetric.HOLD), state.metrics)
        assertEquals(listOf(45.0, 50.0), state.progress!!.chart.points.map { it.value })
        assertEquals(PersonalBestSummaryUi(SetValueUi.Hold(50), LocalDate.of(2026, 8, 8)), state.personalBest)
    }

    @Test
    fun uiState_noSessions_isEmpty() = runTest {
        workouts.sessions.value = emptyMap()

        val state = collectState(createViewModel())().success()

        assertNull(state.progress)
        assertNull(state.personalBest)
        assertEquals(0, state.sessionCount)
        assertTrue(state.sessions.isEmpty())
    }

    @Test
    fun uiState_unknownExercise_isNotFound() = runTest {
        assertEquals(ExerciseDetailsUiState.NotFound, collectState(createViewModel("nope"))())
    }

    @Test
    fun onRenameExercise_renamesTrimmed() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onRenameExercise("  ")
        assertEquals("Bench Press", state().success().name)

        viewModel.onRenameExercise(" Flat Bench ")
        assertEquals("Flat Bench", state().success().name)
    }

    @Test
    fun onRenameExercise_failure_showsMessage() = runTest {
        exercises.renameError = IllegalStateException("disk")
        val viewModel = createViewModel()

        viewModel.onRenameExercise("Flat Bench")

        assertEquals(ExerciseDetailsEvent.ShowMessage(R.string.exercise_error_rename), viewModel.events.first())
        assertFalse(exercises.exercises.value.any { it.name == "Flat Bench" })
    }

    @Test
    fun onExport_usesTheSelectedMetricAndRange() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)
        viewModel.onSelectMetric(ProgressMetric.VOLUME)
        viewModel.onSelectRange(TimeRange.ONE_MONTH)

        viewModel.onExport(ExportFormat.PDF, "content://bench.pdf")

        assertEquals(ExerciseDetailsEvent.ShowMessage(R.string.export_done), viewModel.events.first())
        val report = exporter.exports.single().report
        assertEquals(ReportText.Plain("Bench Press"), report.title)
        val figures = report.sections.first().summary.associate { (it.label as ReportText.Res).id to it.value }
        assertEquals(textCell(R.string.metric_volume), figures[R.string.export_metric])
        assertEquals(textCell(R.string.range_one_month_description), figures[R.string.export_range])
        // Only Sep 5 falls in the month before Oct 3.
        assertEquals(1, report.sections[1].table!!.rows.size)
        assertFalse(viewModel.isExporting.value)
    }

    @Test
    fun onExport_failure_showsMessage() = runTest {
        exporter.error = IOException("disk full")
        val viewModel = createViewModel()

        viewModel.onExport(ExportFormat.XLSX, "content://bench.xlsx")

        assertEquals(ExerciseDetailsEvent.ShowMessage(R.string.export_error), viewModel.events.first())
    }

    @Test
    fun exportFileName_usesTheExerciseName() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        assertEquals("tracker-bench-press-2026-10-03.xlsx", viewModel.exportFileName(ExportFormat.XLSX))
    }

    private fun logBodyweight(vararg entries: Pair<LocalDate, Double>) {
        stats.stats.value = listOf(CustomStat("weight", "Weight", "kg", 1, null, StatIcon.SCALE, StatColor.BLUE))
        stats.entries.value = entries.associate { (date, kg) -> ("weight" to date) to StatEntry("weight", date, kg) }
    }

    @Test
    fun relativeStrength_isOfferedOnceBodyweightIsLogged() = runTest {
        val state = collectState(createViewModel())
        assertFalse(ProgressMetric.RELATIVE_STRENGTH in state().success().metrics)

        logBodyweight(LocalDate.of(2026, 8, 1) to 80.0)

        assertEquals(ProgressMetric.RELATIVE_STRENGTH, state().success().metrics.last())
    }

    @Test
    fun relativeStrength_dividesEachTopSetByThatDaysBodyweight() = runTest {
        logBodyweight(LocalDate.of(2026, 8, 1) to 80.0, LocalDate.of(2026, 8, 22) to 82.0)
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSelectMetric(ProgressMetric.RELATIVE_STRENGTH)

        assertEquals(
            listOf(40.0 / 80, 40.0 / 80, 42.5 / 80, 42.5 / 82, 45.0 / 82, 50.0 / 82),
            state().success().progress!!.chart.points.map { it.value },
        )
    }

    @Test
    fun aStatThatIsntBodyweight_doesNotCount() = runTest {
        stats.stats.value = listOf(CustomStat("waist", "Waist", "cm", 1, null, StatIcon.RULER, StatColor.BLUE))
        stats.entries.value = mapOf(("waist" to LocalDate.of(2026, 8, 1)) to StatEntry("waist", LocalDate.of(2026, 8, 1), 80.0))

        val state = collectState(createViewModel())()

        assertFalse(ProgressMetric.RELATIVE_STRENGTH in state.success().metrics)
    }
}
