package com.example.tracker.data.export

import android.content.Context
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import com.example.tracker.R
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportExporter
import com.example.tracker.domain.export.ReportText
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes reports to content URIs from the system file picker, with texts in the app's language. */
class AndroidReportExporter(context: Context) : ReportExporter {
    private val context = context.applicationContext

    override suspend fun export(report: Report, format: ExportFormat, destination: String) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(destination.toUri(), "wt")
            ?: throw IOException("Can't open $destination")
        output.use { stream ->
            when (format) {
                ExportFormat.XLSX -> XlsxWriter(::resolve).write(report, stream)
                ExportFormat.PDF -> PdfReportWriter(
                    resolve = ::resolve,
                    locale = locale(),
                    footer = { page -> context.getString(R.string.export_pdf_footer, page) },
                ).write(report, stream)
            }
        }
    }

    // getString takes its format arguments as a vararg, so the list has to be spread.
    @Suppress("SpreadOperator")
    private fun resolve(text: ReportText): String = when (text) {
        is ReportText.Plain -> text.text
        is ReportText.Res -> context.getString(text.id, *text.args.map(::formatArg).toTypedArray())
    }

    /** Dates, decimals and nested texts become display text; other values pass through. */
    private fun formatArg(arg: Any): Any = when (arg) {
        is ReportText -> resolve(arg)
        is LocalDate -> arg.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()))
        is Double -> DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(locale())).format(arg)
        else -> arg
    }

    private fun locale(): Locale =
        ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
}
