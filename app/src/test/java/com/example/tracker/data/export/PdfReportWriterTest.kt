package com.example.tracker.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric can't render a PdfDocument, so this covers the layout maths only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfReportWriterTest {

    @Test
    fun columnWidths_tooWide_shrinkInProportionToFitTheWidth() {
        val widths = PdfReportWriter.columnWidths(
            headers = listOf(10f, 10f),
            rows = listOf(listOf(300f, 20f)),
            available = 200f,
        )

        assertEquals(200f, widths.sum(), 0.01f)
        assertTrue(widths[0] > widths[1])
    }

    @Test
    fun columnWidths_narrow_shareTheSpareRoomEvenly() {
        val widths = PdfReportWriter.columnWidths(listOf(10f, 30f), listOf(listOf(150f, 40f)), available = 400f)

        assertEquals(400f, widths.sum(), 0.01f)
        assertEquals(widths[0] - widths[1], 162f - 100f, 0.01f)
    }
}
