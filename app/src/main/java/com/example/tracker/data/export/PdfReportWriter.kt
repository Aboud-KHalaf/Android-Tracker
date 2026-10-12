package com.example.tracker.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportTable
import com.example.tracker.domain.export.ReportText
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Writes a [Report] as an A4 PDF: a title, then each section's figures and table, breaking
 * onto new pages as needed and repeating a table's header on each page it spans.
 * Texts are resolved through [resolve]; numbers and dates are formatted for [locale].
 */
class PdfReportWriter(
    private val resolve: (ReportText) -> String,
    private val locale: Locale,
    private val footer: (page: Int) -> String,
) {
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    private val titlePaint = textPaint(TITLE_SIZE, BRAND, bold = true)
    private val subtitlePaint = textPaint(BODY_SIZE, MUTED)
    private val headingPaint = textPaint(HEADING_SIZE, BRAND, bold = true)
    private val labelPaint = textPaint(BODY_SIZE, MUTED)
    private val valuePaint = textPaint(BODY_SIZE, INK, bold = true)
    private val headerPaint = textPaint(TABLE_SIZE, INK, bold = true)
    private val cellPaint = textPaint(TABLE_SIZE, INK)
    private val footerPaint = textPaint(FOOTER_SIZE, MUTED)
    private val headerFill = Paint().apply { color = HEADER_FILL }
    private val stripeFill = Paint().apply { color = STRIPE_FILL }
    private val rule = Paint().apply { color = BRAND; strokeWidth = 1.5f }

    fun write(report: Report, output: OutputStream) {
        val document = PdfDocument()
        try {
            Layout(document).apply {
                drawTitle(report)
                report.sections.forEach { drawSection(it) }
                finish()
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    /** Tracks the current page and how far down it has been filled. */
    private inner class Layout(private val document: PdfDocument) {
        private var pageNumber = 0
        private var page: PdfDocument.Page = newPage()
        private val canvas: Canvas get() = page.canvas
        private var y = MARGIN

        private fun newPage(): PdfDocument.Page {
            pageNumber++
            return document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        }

        private fun endPage() {
            canvas.drawText(footer(pageNumber), MARGIN, PAGE_HEIGHT - MARGIN / 2, footerPaint)
            document.finishPage(page)
        }

        /** Starts a new page unless [height] still fits on this one. */
        private fun ensureSpace(height: Float): Boolean {
            if (y + height <= BOTTOM) return false
            endPage()
            page = newPage()
            y = MARGIN
            return true
        }

        fun finish() = endPage()

        fun drawTitle(report: Report) {
            y += TITLE_SIZE
            drawLine(resolve(report.title), MARGIN, CONTENT_WIDTH, titlePaint)
            report.subtitle?.let {
                y += LINE_GAP + BODY_SIZE
                drawLine(resolve(it), MARGIN, CONTENT_WIDTH, subtitlePaint)
            }
            y += LINE_GAP
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, rule)
            y += SECTION_GAP
        }

        fun drawSection(section: ReportSection) {
            // Keep a heading with at least its first lines.
            ensureSpace(HEADING_SIZE + LINE_GAP + ROW_HEIGHT * 3)
            y += HEADING_SIZE
            drawLine(resolve(section.title), MARGIN, CONTENT_WIDTH, headingPaint)
            y += LINE_GAP * 2

            section.summary.forEach { entry ->
                ensureSpace(ROW_HEIGHT)
                val baseline = y + ROW_HEIGHT - ROW_PADDING
                val labelWidth = CONTENT_WIDTH * SUMMARY_LABEL_SHARE
                drawText(resolve(entry.label), MARGIN, baseline, labelWidth - CELL_PADDING, labelPaint)
                drawText(format(entry.value), MARGIN + labelWidth, baseline, CONTENT_WIDTH - labelWidth, valuePaint)
                y += ROW_HEIGHT
            }
            section.table?.let {
                if (section.summary.isNotEmpty()) y += LINE_GAP * 2
                drawTable(it)
            }
            y += SECTION_GAP
        }

        private fun drawTable(table: ReportTable) {
            val headers = table.headers.map(resolve)
            val rows = table.rows.map { row -> row.map { format(it) } }
            val numeric = table.headers.indices.map { column ->
                table.rows.any { it[column].isNumeric() } && table.rows.all { it[column].isNumeric() || it[column] == ReportCell.Empty }
            }
            val widths = columnWidths(
                headers.map { headerPaint.measureText(it) },
                rows.map { row -> row.map { cellPaint.measureText(it) } },
                CONTENT_WIDTH,
            )

            ensureSpace(ROW_HEIGHT * 2)
            drawRow(headers, widths, numeric, headerPaint, headerFill)
            rows.forEachIndexed { index, row ->
                if (ensureSpace(ROW_HEIGHT)) drawRow(headers, widths, numeric, headerPaint, headerFill)
                drawRow(row, widths, numeric, cellPaint, if (index % 2 == 1) stripeFill else null)
            }
        }

        private fun drawRow(cells: List<String>, widths: List<Float>, numeric: List<Boolean>, paint: TextPaint, fill: Paint?) {
            fill?.let { canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + ROW_HEIGHT, it) }
            val baseline = y + ROW_HEIGHT - ROW_PADDING
            var x = MARGIN
            cells.forEachIndexed { column, text ->
                val width = widths[column] - CELL_PADDING * 2
                val fitted = TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()
                val start = if (numeric[column]) x + widths[column] - CELL_PADDING - paint.measureText(fitted) else x + CELL_PADDING
                canvas.drawText(fitted, start, baseline, paint)
                x += widths[column]
            }
            y += ROW_HEIGHT
        }

        private fun drawLine(text: String, x: Float, width: Float, paint: TextPaint) = drawText(text, x, y, width, paint)

        private fun drawText(text: String, x: Float, baseline: Float, width: Float, paint: TextPaint) {
            val fitted = TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()
            canvas.drawText(fitted, x, baseline, paint)
        }
    }

    private fun format(cell: ReportCell): String = when (cell) {
        is ReportCell.Text -> resolve(cell.text)
        is ReportCell.Number -> DecimalFormat(
            if (cell.maxDecimals > 0) "#,##0." + "#".repeat(cell.maxDecimals) else "#,##0",
            DecimalFormatSymbols.getInstance(locale),
        ).format(cell.value)
        is ReportCell.Date -> cell.date.format(dateFormat)
        ReportCell.Empty -> ""
    }

    private fun ReportCell.isNumeric() = this is ReportCell.Number

    private fun textPaint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    companion object {
        // A4 in PostScript points.
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 40f
        private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        private const val BOTTOM = PAGE_HEIGHT - MARGIN * 1.5f

        private const val TITLE_SIZE = 20f
        private const val HEADING_SIZE = 14f
        private const val BODY_SIZE = 11f
        private const val TABLE_SIZE = 9.5f
        private const val FOOTER_SIZE = 8f

        private const val LINE_GAP = 6f
        private const val SECTION_GAP = 22f
        private const val ROW_HEIGHT = 18f
        private const val ROW_PADDING = 5f
        private const val CELL_PADDING = 6f
        private const val SUMMARY_LABEL_SHARE = 0.45f

        // The app's blue brand, on white paper whatever the app theme.
        private val BRAND = Color.rgb(0x15, 0x65, 0xC0)
        private val INK = Color.rgb(0x1A, 0x1C, 0x1E)
        private val MUTED = Color.rgb(0x5F, 0x63, 0x68)
        private val HEADER_FILL = Color.rgb(0xDC, 0xE6, 0xF7)
        private val STRIPE_FILL = Color.rgb(0xF3, 0xF6, 0xFB)

        /**
         * Splits [available] width between columns by their widest content (header or cell),
         * so short columns stay compact and no column gets less than a fair minimum.
         */
        internal fun columnWidths(headers: List<Float>, rows: List<List<Float>>, available: Float): List<Float> {
            if (headers.isEmpty()) return emptyList()
            val padding = CELL_PADDING * 2
            val natural = headers.indices.map { column ->
                maxOf(headers[column], rows.maxOfOrNull { it[column] } ?: 0f) + padding
            }
            val minimum = available / headers.size / 2
            val wanted = natural.map { maxOf(it, minimum) }
            val total = wanted.sum()
            return if (total <= available) {
                // Share the slack so the table spans the page.
                wanted.map { it + (available - total) / wanted.size }
            } else {
                wanted.map { it / total * available }
            }
        }
    }
}
