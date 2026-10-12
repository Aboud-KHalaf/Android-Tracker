package com.example.tracker.data.export

import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportText
import java.io.OutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a [Report] as an Excel workbook (.xlsx), one sheet per section, without a library:
 * an .xlsx is a zip of a few SpreadsheetML files. Numbers and dates are written as real
 * values so they can be summed and sorted. Texts are resolved through [resolve].
 */
class XlsxWriter(private val resolve: (ReportText) -> String) {

    fun write(report: Report, output: OutputStream) {
        val sheets = report.sections.ifEmpty { listOf(ReportSection(title = report.title)) }
        val names = sheetNames(sheets.map { resolve(it.title) })
        ZipOutputStream(output).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(sheets.size))
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbook(names))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { index, section ->
                zip.put("xl/worksheets/sheet${index + 1}.xml", worksheet(report, section))
            }
        }
    }

    private fun worksheet(report: Report, section: ReportSection): String {
        val rows = mutableListOf<List<Cell>>()
        rows += listOf(Cell.Str(resolve(report.title), Style.TITLE))
        report.subtitle?.let { rows += listOf(Cell.Str(resolve(it))) }
        rows += emptyList<Cell>()
        rows += listOf(Cell.Str(resolve(section.title), Style.BOLD))
        section.summary.forEach { entry ->
            rows += listOf(Cell.Str(resolve(entry.label)), entry.value.toCell())
        }
        section.table?.let { table ->
            rows += emptyList<Cell>()
            rows += table.headers.map { Cell.Str(resolve(it), Style.HEADER) }
            table.rows.forEach { row -> rows += row.map { it.toCell() } }
        }

        return buildString {
            append(XML_HEADER)
            append("""<worksheet xmlns="$MAIN_NS">""")
            append(columns(rows))
            append("<sheetData>")
            rows.forEachIndexed { rowIndex, cells ->
                val rowNumber = rowIndex + 1
                append("""<row r="$rowNumber">""")
                cells.forEachIndexed { column, cell ->
                    append(cell.xml("${columnName(column)}$rowNumber"))
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
    }

    /** Widths fitted to the longest value in each column. */
    private fun columns(rows: List<List<Cell>>): String {
        val count = rows.maxOfOrNull { it.size } ?: 0
        if (count == 0) return ""
        return buildString {
            append("<cols>")
            for (column in 0 until count) {
                // The title row spans the sheet, so it doesn't widen the first column.
                val longest = rows.drop(1).mapNotNull { it.getOrNull(column)?.width }.maxOrNull() ?: 0
                val width = (longest + 2).coerceIn(MIN_WIDTH, MAX_WIDTH)
                append("""<col min="${column + 1}" max="${column + 1}" width="$width" customWidth="1"/>""")
            }
            append("</cols>")
        }
    }

    private fun ReportCell.toCell(): Cell = when (this) {
        is ReportCell.Text -> Cell.Str(resolve(text))
        is ReportCell.Number -> Cell.Num(value, if (maxDecimals > 0) Style.DECIMAL else Style.INTEGER)
        is ReportCell.Date -> Cell.Day(date)
        ReportCell.Empty -> Cell.Blank
    }

    private sealed interface Cell {
        val width: Int
        fun xml(ref: String): String

        data class Str(val text: String, val style: Int = Style.DEFAULT) : Cell {
            override val width get() = text.length
            override fun xml(ref: String) =
                """<c r="$ref" t="inlineStr"${styleAttr(style)}><is><t xml:space="preserve">${escape(text)}</t></is></c>"""
        }

        data class Num(val value: Double, val style: Int) : Cell {
            override val width get() = plain(value).length + 2
            override fun xml(ref: String) = """<c r="$ref"${styleAttr(style)}><v>${plain(value)}</v></c>"""
        }

        data class Day(val date: LocalDate) : Cell {
            override val width get() = DATE_WIDTH
            override fun xml(ref: String) =
                """<c r="$ref"${styleAttr(Style.DATE)}><v>${ChronoUnit.DAYS.between(EXCEL_EPOCH, date)}</v></c>"""
        }

        data object Blank : Cell {
            override val width get() = 0
            override fun xml(ref: String) = ""
        }
    }

    /** Indexes into cellXfs in [STYLES]. */
    private object Style {
        const val DEFAULT = 0
        const val BOLD = 1
        const val HEADER = 2
        const val DATE = 3
        const val INTEGER = 4
        const val DECIMAL = 5
        const val TITLE = 6
    }

    companion object {
        private const val MAX_SHEET_NAME = 31
        private const val MIN_WIDTH = 8
        private const val MAX_WIDTH = 60
        private const val DATE_WIDTH = 12

        /** Day zero of Excel's 1900 date system, accounting for its 1900 leap-year bug. */
        private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)

        private const val XML_HEADER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        private const val PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"

        private const val ROOT_RELS = XML_HEADER +
            """<Relationships xmlns="$PKG_REL_NS">""" +
            """<Relationship Id="rId1" Type="$REL_NS/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"

        private const val STYLES = XML_HEADER +
            """<styleSheet xmlns="$MAIN_NS">""" +
            """<numFmts count="1"><numFmt numFmtId="164" formatCode="#,##0.0"/></numFmts>""" +
            """<fonts count="3">""" +
            """<font><sz val="11"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="14"/><color rgb="FF1565C0"/><name val="Calibri"/></font>""" +
            "</fonts>" +
            """<fills count="3">""" +
            """<fill><patternFill patternType="none"/></fill>""" +
            """<fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FFDCE6F7"/><bgColor indexed="64"/></patternFill></fill>""" +
            "</fills>" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="7">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
            """<xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
            """<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
            "</cellXfs>" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            "</styleSheet>"

        private fun contentTypes(sheetCount: Int) = buildString {
            append(XML_HEADER)
            append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
            append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
            append("""<Default Extension="xml" ContentType="application/xml"/>""")
            append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
            append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
            for (i in 1..sheetCount) {
                append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
            }
            append("</Types>")
        }

        private fun workbook(names: List<String>) = buildString {
            append(XML_HEADER)
            append("""<workbook xmlns="$MAIN_NS" xmlns:r="$REL_NS"><sheets>""")
            names.forEachIndexed { index, name ->
                append("""<sheet name="${escape(name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
            }
            append("</sheets></workbook>")
        }

        private fun workbookRels(sheetCount: Int) = buildString {
            append(XML_HEADER)
            append("""<Relationships xmlns="$PKG_REL_NS">""")
            for (i in 1..sheetCount) {
                append("""<Relationship Id="rId$i" Type="$REL_NS/worksheet" Target="worksheets/sheet$i.xml"/>""")
            }
            append("""<Relationship Id="rId${sheetCount + 1}" Type="$REL_NS/styles" Target="styles.xml"/>""")
            append("</Relationships>")
        }

        /** Excel's rules: 1–31 characters, none of []:*?/\, unique ignoring case. */
        internal fun sheetNames(titles: List<String>): List<String> {
            val used = mutableSetOf<String>()
            return titles.mapIndexed { index, title ->
                val base = title.replace(Regex("""[\[\]:*?/\\]"""), " ").trim().trim('\'')
                    .ifEmpty { "Sheet${index + 1}" }
                    .take(MAX_SHEET_NAME)
                var name = base
                var suffix = 2
                while (!used.add(name.lowercase(Locale.ROOT))) {
                    val tail = " ($suffix)"
                    name = base.take(MAX_SHEET_NAME - tail.length) + tail
                    suffix++
                }
                name
            }
        }

        /** "A", "B", …, "Z", "AA". */
        internal fun columnName(index: Int): String {
            var n = index + 1
            val name = StringBuilder()
            while (n > 0) {
                val rem = (n - 1) % 26
                name.insert(0, 'A' + rem)
                n = (n - 1) / 26
            }
            return name.toString()
        }

        private fun styleAttr(style: Int) = if (style == Style.DEFAULT) "" else """ s="$style""""

        private fun plain(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

        /** Escapes markup and drops characters XML 1.0 can't hold. */
        private fun escape(text: String): String = buildString {
            text.forEach { c ->
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\t', '\n', '\r' -> append(c)
                    else -> if (c >= ' ' && c != '￾' && c != '￿') append(c)
                }
            }
        }

        private fun ZipOutputStream.put(name: String, content: String) {
            putNextEntry(ZipEntry(name))
            write(content.toByteArray(Charsets.UTF_8))
            closeEntry()
        }
    }
}
