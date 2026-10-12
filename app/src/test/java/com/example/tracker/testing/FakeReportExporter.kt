package com.example.tracker.testing

import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportExporter

class FakeReportExporter : ReportExporter {
    data class Export(val report: Report, val format: ExportFormat, val destination: String)

    val exports = mutableListOf<Export>()

    /** When set, every export fails with it. */
    var error: Exception? = null

    override suspend fun export(report: Report, format: ExportFormat, destination: String) {
        error?.let { throw it }
        exports += Export(report, format, destination)
    }
}
