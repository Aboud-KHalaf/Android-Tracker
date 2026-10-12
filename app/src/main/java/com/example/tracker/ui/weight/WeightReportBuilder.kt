package com.example.tracker.ui.weight

import com.example.tracker.R
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportEntry
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportTable
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.text
import com.example.tracker.domain.model.WeightEntry
import com.example.tracker.domain.weight.WeightSummary
import java.time.LocalDate

/** Builds the weight report: the period's summary, then every entry oldest first. Pure. */
internal object WeightReportBuilder {

    fun build(from: LocalDate, to: LocalDate, exportedOn: LocalDate, entries: List<WeightEntry>): Report =
        Report(
            title = text(R.string.weight_title),
            subtitle = text(R.string.export_period_exported_on, from, to, exportedOn),
            sections = sections(entries),
        )

    /** The summary and entries sections, shared with the full export. */
    fun sections(entries: List<WeightEntry>): List<ReportSection> {
        val summary = WeightSummary.of(entries)
        val oldestFirst = entries.sortedBy { it.date }
        return listOf(
            ReportSection(
                title = text(R.string.weight_title),
                summary = buildList {
                    add(ReportEntry(text(R.string.export_entry_count), numberCell(entries.size)))
                    if (summary != null) {
                        add(ReportEntry(text(R.string.export_starting_weight), numberCell(summary.first.weightKg, 1)))
                        add(ReportEntry(text(R.string.export_latest_weight), numberCell(summary.latest.weightKg, 1)))
                        add(ReportEntry(text(R.string.export_column_change_kg), numberCell(summary.changeKg, 1)))
                        add(ReportEntry(text(R.string.export_lowest_weight), numberCell(summary.lowestKg, 1)))
                        add(ReportEntry(text(R.string.export_highest_weight), numberCell(summary.highestKg, 1)))
                    }
                },
            ),
            ReportSection(
                title = text(R.string.export_section_entries),
                table = ReportTable(
                    headers = listOf(
                        text(R.string.export_column_date),
                        text(R.string.export_column_weight_kg),
                        text(R.string.export_column_change_kg),
                    ),
                    rows = oldestFirst.mapIndexed { i, entry ->
                        val change = oldestFirst.getOrNull(i - 1)?.let { entry.weightKg - it.weightKg }
                        listOf(ReportCell.Date(entry.date), numberCell(entry.weightKg, 1), numberCell(change, 1))
                    },
                ),
            ),
        )
    }
}
