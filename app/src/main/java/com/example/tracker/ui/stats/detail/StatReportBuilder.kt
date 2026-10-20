package com.example.tracker.ui.stats.detail

import com.example.tracker.R
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportEntry
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportTable
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.text
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.stats.StatSummary
import java.time.LocalDate

/** Builds a stat's report: the period's summary, then every entry oldest first. Pure. */
internal object StatReportBuilder {

    fun build(stat: CustomStat, from: LocalDate, to: LocalDate, exportedOn: LocalDate, entries: List<StatEntry>): Report =
        Report(
            title = ReportText.Plain(stat.name),
            subtitle = text(R.string.export_period_exported_on, from, to, exportedOn),
            sections = sections(stat, entries),
        )

    /** The summary and entries sections, shared with the full export. */
    fun sections(stat: CustomStat, entries: List<StatEntry>): List<ReportSection> {
        val summary = StatSummary.of(entries)
        val oldestFirst = entries.sortedBy { it.date }
        val decimals = stat.decimals
        val name = ReportText.Plain(stat.name)
        val valueHeader = if (stat.unit.isBlank()) name else text(R.string.export_column_with_unit, stat.name, stat.unit)
        return listOf(
            ReportSection(
                title = name,
                summary = buildList {
                    add(ReportEntry(text(R.string.export_entry_count), numberCell(entries.size)))
                    if (summary != null) {
                        add(ReportEntry(text(R.string.export_stat_first), numberCell(summary.first.value, decimals)))
                        add(ReportEntry(text(R.string.export_stat_latest), numberCell(summary.latest.value, decimals)))
                        add(ReportEntry(text(R.string.export_stat_change), numberCell(summary.change, decimals)))
                        add(ReportEntry(text(R.string.export_stat_lowest), numberCell(summary.lowest, decimals)))
                        add(ReportEntry(text(R.string.export_stat_highest), numberCell(summary.highest, decimals)))
                    }
                    stat.target?.let { add(ReportEntry(text(R.string.export_stat_target), numberCell(it, decimals))) }
                },
            ),
            ReportSection(
                title = text(R.string.export_stat_entries, stat.name),
                table = ReportTable(
                    headers = listOf(text(R.string.export_column_date), valueHeader, text(R.string.export_stat_change)),
                    rows = oldestFirst.mapIndexed { i, entry ->
                        val change = oldestFirst.getOrNull(i - 1)?.let { entry.value - it.value }
                        listOf(ReportCell.Date(entry.date), numberCell(entry.value, decimals), numberCell(change, decimals))
                    },
                ),
            ),
        )
    }
}
