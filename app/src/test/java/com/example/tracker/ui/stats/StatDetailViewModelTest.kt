package com.example.tracker.ui.stats

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeReportExporter
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.stats.detail.StatDetailEvent
import com.example.tracker.ui.stats.detail.StatDetailUiState
import com.example.tracker.ui.stats.detail.StatDetailViewModel
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
class StatDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCustomStatRepository()
    private val exporter = FakeReportExporter()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 3)

    private val bodyFat = CustomStat("fat", "Body fat", "%", 1, 15.0, StatIcon.PERCENT, StatColor.ORANGE)

    init {
        repository.stats.value = listOf(bodyFat)
    }

    private fun createViewModel(logToday: Boolean = false) = StatDetailViewModel("fat", logToday, repository, exporter, time)

    private fun TestScope.collectState(viewModel: StatDetailViewModel): () -> StatDetailUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    @Test
    fun uiState_last30Days_withChangesAndProgress() = runTest {
        repository.put(
            StatEntry("fat", LocalDate.of(2026, 8, 1), 21.0), // before the period, but the first ever
            StatEntry("fat", today.minusDays(5), 19.0),
            StatEntry("fat", today, 18.0),
        )
        val state = collectState(createViewModel())() as StatDetailUiState.Success

        assertEquals(StatEntry("fat", today, 18.0), state.latest)
        assertEquals(0.5f, state.progress!!, 1e-6f)
        val rows = state.months.flatMap { it.entries }
        assertEquals(listOf(today, today.minusDays(5)), rows.map { it.entry.date })
        assertEquals(-1.0, rows.first().change!!, 1e-9)
        assertNull(rows.last().change)
        assertEquals(-1.0, state.summary!!.change, 1e-9)
    }

    @Test
    fun uiState_missingOnceTheStatIsDeleted() = runTest {
        val state = collectState(createViewModel())
        repository.deleteStat("fat")

        assertEquals(StatDetailUiState.Missing, state())
    }

    @Test
    fun logToday_opensTodayStartingFromTheLatestValue() = runTest {
        repository.put(StatEntry("fat", today.minusDays(2), 19.25))
        val viewModel = createViewModel(logToday = true)

        val editor = viewModel.editor.value!!
        assertEquals(today, editor.date)
        assertEquals("19.3", editor.value)
        assertFalse(editor.isExisting)
    }

    @Test
    fun onSaveEntry_roundsToTheStatsDecimals() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()
        viewModel.onEditorValueChange("18.46")

        viewModel.onSaveEntry()

        assertEquals(StatEntry("fat", today, 18.4), repository.observeEntry("fat", today).first())
        assertNull(viewModel.editor.value)
    }

    @Test
    fun onSaveEntry_emptyValue_showsError() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()
        viewModel.onEditorValueChange("")

        viewModel.onSaveEntry()

        assertTrue(viewModel.editor.value!!.valueError)
    }

    @Test
    fun onEditorDateChange_toLoggedDay_bringsItsValue() = runTest {
        repository.put(StatEntry("fat", today.minusDays(1), 19.0))
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorDateChange(today.minusDays(1))

        assertEquals("19.0", viewModel.editor.value!!.value)
        assertTrue(viewModel.editor.value!!.isExisting)
    }

    @Test
    fun onDeleteEntry_deletesAndOffersUndo() = runTest {
        repository.put(StatEntry("fat", today, 18.0))
        val viewModel = createViewModel()
        viewModel.onOpenEditor(today)

        viewModel.onDeleteEntry()

        assertNull(repository.observeEntry("fat", today).first())
        val event = viewModel.events.first() as StatDetailEvent.EntryDeleted
        viewModel.onUndoDelete(event.entry)
        assertEquals(StatEntry("fat", today, 18.0), repository.observeEntry("fat", today).first())
    }

    @Test
    fun onExport_writesThePeriodsEntries() = runTest {
        repository.put(StatEntry("fat", today.minusDays(3), 19.0), StatEntry("fat", today, 18.0))
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onExport(ExportFormat.PDF, "content://fat.pdf")

        val report = exporter.exports.single().report
        assertEquals(ReportText.Plain("Body fat"), report.title)
        assertEquals(2, report.sections.last().table!!.rows.size)
        assertEquals("tracker-body-fat-2026-10-03.pdf", viewModel.exportFileName(ExportFormat.PDF))
    }
}
