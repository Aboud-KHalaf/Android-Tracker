package com.example.tracker.ui.weight

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.testing.FakeProgressPhotoRepository
import com.example.tracker.testing.FakeReportExporter
import com.example.tracker.testing.FakeWeightRepository
import com.example.tracker.testing.MainDispatcherRule
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val weights = FakeWeightRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 3)

    private val exporter = FakeReportExporter()

    private val photos = FakeProgressPhotoRepository()

    private fun createViewModel() = WeightViewModel(weights, photos, exporter, time)

    private fun TestScope.collectState(viewModel: WeightViewModel): () -> WeightUiState.Success {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value as WeightUiState.Success }
    }

    @Test
    fun uiState_last30DaysByDefault_withChangesBetweenEntries() = runTest {
        weights.put(
            WeightEntry(today, 78.4),
            WeightEntry(today.minusDays(3), 79.0),
            WeightEntry(LocalDate.of(2026, 9, 20), 80.0),
            WeightEntry(LocalDate.of(2026, 8, 1), 85.0), // before the period
        )

        val state = collectState(createViewModel())()

        assertEquals(DatePeriod.Last30Days, state.period)
        assertEquals(WeightEntry(today, 78.4), state.latest)
        assertEquals(listOf(today, today.minusDays(3), LocalDate.of(2026, 9, 20)), state.months.flatMap { it.entries }.map { it.entry.date })
        val changes = state.months.flatMap { it.entries }.map { it.changeKg }
        assertEquals(-0.6, changes[0]!!, 1e-9)
        assertEquals(-1.0, changes[1]!!, 1e-9)
        assertNull(changes[2]) // the first entry in the period has nothing to compare with
        assertEquals(-1.6, state.summary!!.changeKg, 1e-9)
        assertEquals(3, state.chart!!.points.size)
    }

    @Test
    fun uiState_latestComesFromOutsideThePeriodToo() = runTest {
        weights.put(WeightEntry(LocalDate.of(2026, 8, 1), 85.0))

        val state = collectState(createViewModel())()

        assertEquals(WeightEntry(LocalDate.of(2026, 8, 1), 85.0), state.latest)
        assertNull(state.chart)
        assertNull(state.summary)
    }

    @Test
    fun uiState_loadFailure_showsErrorAndRetryRecovers() = runTest {
        weights.observeError = IOException("disk")
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(WeightUiState.Error, viewModel.uiState.value)

        weights.observeError = null
        viewModel.onRetry()

        assertTrue(viewModel.uiState.value is WeightUiState.Success)
    }

    @Test
    fun onOpenEditor_newDay_startsFromLatestWeight() = runTest {
        weights.put(WeightEntry(today.minusDays(2), 79.5))
        val viewModel = createViewModel()

        viewModel.onOpenEditor()

        assertEquals(WeightEditorState(today, "79.5", isExisting = false), viewModel.editor.value)
    }

    @Test
    fun onSaveEntry_acceptsCommaAndSaves() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorWeightChange("78,4")
        viewModel.onSaveEntry()

        assertEquals(WeightEntry(today, 78.4), weights.entries.value[today])
        assertNull(viewModel.editor.value)
    }

    @Test
    fun onSaveEntry_outOfRange_showsErrorAndKeepsDialogOpen() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorWeightChange("8")
        viewModel.onSaveEntry()

        assertTrue(viewModel.editor.value!!.weightError)
        assertTrue(weights.entries.value.isEmpty())
    }

    @Test
    fun onSaveEntry_failure_showsMessage() = runTest {
        weights.writeError = IOException("disk")
        val viewModel = createViewModel()
        viewModel.onOpenEditor()
        viewModel.onEditorWeightChange("78")

        viewModel.onSaveEntry()

        assertEquals(WeightEvent.ShowMessage(R.string.weight_error_save), viewModel.events.first())
        assertFalse(viewModel.editor.value!!.isSaving)
    }

    @Test
    fun onEditorDateChange_toLoggedDay_loadsItsWeight() = runTest {
        weights.put(WeightEntry(today.minusDays(1), 79.2))
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorDateChange(today.minusDays(1))

        assertEquals(WeightEditorState(today.minusDays(1), "79.2", isExisting = true), viewModel.editor.value)
    }

    @Test
    fun onDeleteEntry_deletesAndUndoRestores() = runTest {
        val entry = WeightEntry(today, 78.4)
        weights.put(entry)
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onDeleteEntry()

        assertNull(weights.entries.value[today])
        assertEquals(WeightEvent.EntryDeleted(entry), viewModel.events.first())

        viewModel.onUndoDelete(entry)

        assertEquals(entry, weights.entries.value[today])
    }

    @Test
    fun onExport_writesTheSelectedPeriodAndConfirms() = runTest {
        weights.put(
            WeightEntry(today, 78.4),
            WeightEntry(LocalDate.of(2026, 9, 20), 80.0),
            WeightEntry(LocalDate.of(2026, 8, 1), 85.0), // before the period
        )
        val viewModel = createViewModel()
        viewModel.onSelectPeriod(DatePeriod.ThisMonth)

        viewModel.onExport(ExportFormat.XLSX, "content://weight.xlsx")

        assertEquals(WeightEvent.ShowMessage(R.string.export_done), viewModel.events.first())
        val export = exporter.exports.single()
        assertEquals(ExportFormat.XLSX, export.format)
        assertEquals("content://weight.xlsx", export.destination)
        val dates = export.report.sections.last().table!!.rows.map { it.first() }
        assertEquals(listOf(ReportCell.Date(today)), dates)
        assertFalse(viewModel.isExporting.value)
    }

    @Test
    fun onExport_failure_showsMessage() = runTest {
        exporter.error = IOException("disk full")
        val viewModel = createViewModel()

        viewModel.onExport(ExportFormat.PDF, "content://weight.pdf")

        assertEquals(WeightEvent.ShowMessage(R.string.export_error), viewModel.events.first())
        assertFalse(viewModel.isExporting.value)
    }

    @Test
    fun exportFileName_namesTheSectionAndDay() {
        assertEquals("tracker-weight-2026-10-03.pdf", createViewModel().exportFileName(ExportFormat.PDF))
    }

    @Test
    fun latestCheckIn_isTheNewestDayWithPhotos() = runTest {
        photos.put(
            ProgressPhoto("old", today.minusDays(7), PhotoPose.FRONT, "/old.jpg"),
            ProgressPhoto("front", today, PhotoPose.FRONT, "/front.jpg"),
            ProgressPhoto("back", today, PhotoPose.BACK, "/back.jpg"),
        )
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.latestCheckIn.collect {} }

        val latest = viewModel.latestCheckIn.value

        assertEquals(today, latest?.date)
        assertEquals(setOf(PhotoPose.FRONT, PhotoPose.BACK), latest?.photos?.keys)
    }

    @Test
    fun latestCheckIn_nullWhenPhotosFailToLoad() = runTest {
        photos.observeError = IOException("disk")
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.latestCheckIn.collect {} }

        assertNull(viewModel.latestCheckIn.value)
    }
}
