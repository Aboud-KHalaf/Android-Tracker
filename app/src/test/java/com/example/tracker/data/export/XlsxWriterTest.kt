package com.example.tracker.data.export

import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportEntry
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportTable
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.plainCell
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class XlsxWriterTest {

    private val writer = XlsxWriter { text ->
        when (text) {
            is ReportText.Plain -> text.text
            is ReportText.Res -> "res-${text.id}"
        }
    }

    private fun plain(text: String) = ReportText.Plain(text)

    private val report = Report(
        title = plain("Weight"),
        subtitle = plain("Oct 1 – Oct 12"),
        sections = listOf(
            ReportSection(
                title = plain("Summary"),
                summary = listOf(ReportEntry(plain("Entries"), numberCell(2))),
            ),
            ReportSection(
                title = plain("Entries"),
                table = ReportTable(
                    headers = listOf(plain("Date"), plain("Weight (kg)"), plain("Note")),
                    rows = listOf(
                        listOf(ReportCell.Date(LocalDate.of(2026, 10, 1)), numberCell(80.5, 1), plainCell("Fish & <chips>")),
                        listOf(ReportCell.Date(LocalDate.of(2026, 10, 2)), numberCell(80.0, 1), ReportCell.Empty),
                    ),
                ),
            ),
        ),
    )

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return entries
    }

    private fun parse(xml: String): Element =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray()))
            .documentElement

    private fun write(report: Report): Map<String, String> =
        unzip(ByteArrayOutputStream().also { writer.write(report, it) }.toByteArray())

    @Test
    fun write_producesAWellFormedPackageWithOneSheetPerSection() {
        val files = write(report)

        assertTrue(files.keys.containsAll(
            listOf(
                "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml",
            ),
        ))
        files.values.forEach { parse(it) }
        val sheets = parse(files.getValue("xl/workbook.xml")).getElementsByTagName("sheet")
        assertEquals(2, sheets.length)
        assertEquals("Summary", (sheets.item(0) as Element).getAttribute("name"))
        assertEquals("Entries", (sheets.item(1) as Element).getAttribute("name"))
    }

    @Test
    fun write_keepsNumbersAndDatesAsValuesAndEscapesText() {
        val sheet = write(report).getValue("xl/worksheets/sheet2.xml")

        // Oct 1, 2026 is day 46296 in Excel's 1900 date system.
        assertTrue(sheet.contains("""<c r="A7" s="3"><v>46296</v></c>"""))
        assertTrue(sheet.contains("""<c r="B7" s="5"><v>80.5</v></c>"""))
        assertTrue(sheet.contains("""<c r="B8" s="5"><v>80</v></c>"""))
        assertTrue(sheet.contains("Fish &amp; &lt;chips&gt;"))
    }

    @Test
    fun write_noSections_stillWritesOneSheet() {
        val files = write(Report(title = plain("Empty"), sections = emptyList()))

        assertTrue("xl/worksheets/sheet1.xml" in files)
        parse(files.getValue("xl/workbook.xml"))
    }

    @Test
    fun sheetNames_followExcelRules() {
        val long = "A".repeat(40)

        val names = XlsxWriter.sheetNames(listOf("Week 1/2", "summary", "Summary", long, long, ""))

        assertEquals(listOf("Week 1 2", "summary", "Summary (2)", "A".repeat(31), "A".repeat(27) + " (2)", "Sheet6"), names)
    }

    @Test
    fun columnName_countsLikeExcel() {
        assertEquals(listOf("A", "Z", "AA", "AZ", "BA"), listOf(0, 25, 26, 51, 52).map { XlsxWriter.columnName(it) })
    }
}
