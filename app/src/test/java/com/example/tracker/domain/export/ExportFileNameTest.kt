package com.example.tracker.domain.export

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFileNameTest {

    private val date = LocalDate.of(2026, 10, 12)

    @Test
    fun exportFileName_slugsTheSubject() {
        assertEquals("tracker-bench-press-2026-10-12.pdf", exportFileName("Bench Press!", date, ExportFormat.PDF))
    }

    @Test
    fun exportFileName_blankSubject_usesTheAppName() {
        assertEquals("tracker-2026-10-12.xlsx", exportFileName(" / ", date, ExportFormat.XLSX))
    }
}
