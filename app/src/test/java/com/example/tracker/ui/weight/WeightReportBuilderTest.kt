package com.example.tracker.ui.weight

import com.example.tracker.R
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.model.WeightEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightReportBuilderTest {

    private val oct1 = LocalDate.of(2026, 10, 1)

    @Test
    fun build_summarizesAndListsEntriesOldestFirstWithChanges() {
        val entries = listOf(WeightEntry(oct1.plusDays(2), 79.5), WeightEntry(oct1, 80.0))

        val report = WeightReportBuilder.build(oct1, oct1.plusDays(10), oct1.plusDays(11), entries)

        assertEquals(ReportText.Res(R.string.weight_title), report.title)
        val (summary, list) = report.sections
        val figures = summary.summary.associate { (it.label as ReportText.Res).id to it.value }
        assertEquals(numberCell(2), figures[R.string.export_entry_count])
        assertEquals(numberCell(-0.5, 1), figures[R.string.export_column_change_kg])
        assertEquals(
            listOf(
                listOf(ReportCell.Date(oct1), numberCell(80.0, 1), ReportCell.Empty),
                listOf(ReportCell.Date(oct1.plusDays(2)), numberCell(79.5, 1), numberCell(-0.5, 1)),
            ),
            list.table!!.rows,
        )
    }

    @Test
    fun build_noEntries_hasOnlyTheCountAndAnEmptyTable() {
        val report = WeightReportBuilder.build(oct1, oct1, oct1, emptyList())

        assertEquals(1, report.sections.first().summary.size)
        assertTrue(report.sections.last().table!!.rows.isEmpty())
    }
}
