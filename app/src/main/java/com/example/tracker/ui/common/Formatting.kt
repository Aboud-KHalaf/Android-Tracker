package com.example.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import com.example.tracker.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The locale of the current configuration, so text updates when the user changes language. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

/** "50" or "47.5": whole kilos without decimals, otherwise one decimal. */
fun formatWeight(kg: Double, locale: Locale): String =
    DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale)).format(kg)

/** "0:45", "1:05". */
fun formatClock(totalSeconds: Int): String =
    "%d:%02d".format(Locale.ROOT, totalSeconds / 60, totalSeconds % 60)

/** "55 min" or "2 h 35 min". */
@Composable
@ReadOnlyComposable
fun formatDuration(duration: Duration): String {
    val hours = duration.toHours().toInt()
    val minutes = duration.toMinutes().toInt() % 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

/** "Mon". */
fun DayOfWeek.shortName(locale: Locale): String = getDisplayName(TextStyle.SHORT, locale)

/** "M" for the week strip. */
fun DayOfWeek.narrowName(locale: Locale): String = getDisplayName(TextStyle.NARROW, locale)

/** "Monday". */
fun DayOfWeek.fullName(locale: Locale): String = getDisplayName(TextStyle.FULL, locale)

/** "OCT", for date tiles. */
fun LocalDate.shortMonth(locale: Locale): String =
    month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)

/** "Saturday, October 3". */
fun LocalDate.longDate(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))

/** "Mon, Sep 28". */
fun LocalDate.mediumDate(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
