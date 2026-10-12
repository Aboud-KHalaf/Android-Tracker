package com.example.tracker.domain.export

import java.time.LocalDate
import java.util.Locale

/** Writes a [Report] to a file the user picked. */
interface ReportExporter {
    /**
     * Writes [report] as [format] to [destination], a content URI from the system file picker.
     * Throws when the file can't be written.
     */
    suspend fun export(report: Report, format: ExportFormat, destination: String)
}

/** A suggested file name: "tracker-bench-press-2026-10-12.pdf". */
fun exportFileName(subject: String, date: LocalDate, format: ExportFormat): String {
    val slug = subject.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
    val prefix = if (slug.isEmpty()) "tracker" else "tracker-$slug"
    return "$prefix-$date.${format.extension}"
}
