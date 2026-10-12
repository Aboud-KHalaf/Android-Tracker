package com.example.tracker.domain.export

import androidx.annotation.StringRes
import java.time.LocalDate

/**
 * A document to export, independent of its file format: a title, then sections that each hold
 * a few headline figures and a table. A section becomes a block in a PDF and a sheet in a workbook.
 */
data class Report(
    val title: ReportText,
    val subtitle: ReportText? = null,
    val sections: List<ReportSection>,
)

data class ReportSection(
    val title: ReportText,
    /** Headline figures shown above the table, as label and value. */
    val summary: List<ReportEntry> = emptyList(),
    /** Null when there is nothing to list, e.g. no entries in the period. */
    val table: ReportTable? = null,
)

data class ReportEntry(val label: ReportText, val value: ReportCell)

data class ReportTable(
    val headers: List<ReportText>,
    val rows: List<List<ReportCell>>,
) {
    init {
        require(rows.all { it.size == headers.size }) { "Every row needs one cell per header" }
    }
}

/** One value. Numbers and dates stay typed so a spreadsheet can sum and sort them. */
sealed interface ReportCell {
    data class Text(val text: ReportText) : ReportCell
    data class Number(val value: Double, val maxDecimals: Int = 0) : ReportCell
    data class Date(val date: LocalDate) : ReportCell
    data object Empty : ReportCell
}

/** Text resolved when the file is written, so labels follow the app's language. */
sealed interface ReportText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : ReportText
    data class Plain(val text: String) : ReportText
}

fun text(@StringRes id: Int, vararg args: Any): ReportText = ReportText.Res(id, args.toList())

fun textCell(@StringRes id: Int, vararg args: Any): ReportCell = ReportCell.Text(ReportText.Res(id, args.toList()))

fun plainCell(text: String): ReportCell = ReportCell.Text(ReportText.Plain(text))

fun numberCell(value: Number?, maxDecimals: Int = 0): ReportCell =
    value?.let { ReportCell.Number(it.toDouble(), maxDecimals) } ?: ReportCell.Empty
