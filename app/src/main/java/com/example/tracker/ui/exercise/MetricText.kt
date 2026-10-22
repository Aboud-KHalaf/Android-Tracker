package com.example.tracker.ui.exercise

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatClock
import com.example.tracker.ui.common.formatNumber
import com.example.tracker.ui.common.shortDate
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** "Weight", "1RM", "Reps", "Volume" or "Longest hold". */
@Composable
@ReadOnlyComposable
fun ProgressMetric.label(): String = stringResource(labelRes)

@get:StringRes
internal val ProgressMetric.labelRes: Int
    get() = when (this) {
        ProgressMetric.WEIGHT -> R.string.metric_weight
        ProgressMetric.ONE_REP_MAX -> R.string.metric_one_rep_max
        ProgressMetric.REPS -> R.string.metric_reps
        ProgressMetric.VOLUME -> R.string.metric_volume
        ProgressMetric.HOLD -> R.string.metric_hold
        ProgressMetric.RELATIVE_STRENGTH -> R.string.metric_relative_strength
    }

/** A value with its unit: "50 kg", "24 reps", "1,250 kg" or "0:50". */
@Composable
@ReadOnlyComposable
fun ProgressMetric.valueText(value: Double): String {
    val number = formatNumber(value, currentLocale())
    return when (this) {
        ProgressMetric.WEIGHT, ProgressMetric.ONE_REP_MAX, ProgressMetric.VOLUME ->
            stringResource(R.string.value_kg, number)
        ProgressMetric.REPS -> pluralStringResource(R.plurals.value_reps, value.roundToInt(), number)
        ProgressMetric.HOLD -> formatClock(value.roundToInt())
        ProgressMetric.RELATIVE_STRENGTH -> stringResource(R.string.value_bodyweight, formatRatio(value, currentLocale()))
    }
}

/** "1.25": two decimals, so small gains in a bodyweight multiple still show. */
private fun formatRatio(value: Double, locale: Locale): String =
    DecimalFormat("0.00", DecimalFormatSymbols.getInstance(locale)).format(value)

/** A y-axis label without the unit: "50", "1,250" or "0:50". */
@Composable
@ReadOnlyComposable
fun ProgressMetric.axisText(value: Double): String =
    when (this) {
        ProgressMetric.HOLD -> formatClock(value.roundToInt())
        ProgressMetric.RELATIVE_STRENGTH -> formatRatio(value, currentLocale())
        else -> formatNumber(value, currentLocale())
    }

/** A signed change: "+10 kg", "−3 reps", "+5 s", or "No change". */
@Composable
@ReadOnlyComposable
fun ProgressMetric.changeText(change: Double): String {
    if (change == 0.0) return stringResource(R.string.change_none)
    val sign = if (change > 0) "+" else "−"
    val size = abs(change)
    return when (this) {
        ProgressMetric.HOLD -> stringResource(R.string.change_hold, sign, size.roundToInt())
        else -> sign + valueText(size)
    }
}

/** "Top set weight since Aug 1". */
@Composable
@ReadOnlyComposable
fun ProgressMetric.sinceText(since: LocalDate): String {
    val date = since.shortDate(currentLocale())
    return stringResource(
        when (this) {
            ProgressMetric.WEIGHT -> R.string.metric_weight_since
            ProgressMetric.ONE_REP_MAX -> R.string.metric_one_rep_max_since
            ProgressMetric.REPS -> R.string.metric_reps_since
            ProgressMetric.VOLUME -> R.string.metric_volume_since
            ProgressMetric.HOLD -> R.string.metric_hold_since
            ProgressMetric.RELATIVE_STRENGTH -> R.string.metric_relative_strength_since
        },
        date,
    )
}

/** "1M", "3M", "1Y", "All". */
@Composable
@ReadOnlyComposable
fun TimeRange.label(): String = stringResource(
    when (this) {
        TimeRange.ONE_MONTH -> R.string.range_one_month
        TimeRange.THREE_MONTHS -> R.string.range_three_months
        TimeRange.ONE_YEAR -> R.string.range_one_year
        TimeRange.ALL -> R.string.range_all
    },
)

/** Spoken form of [label]: "Last 3 months". */
@Composable
@ReadOnlyComposable
fun TimeRange.description(): String = stringResource(descriptionRes)

@get:StringRes
internal val TimeRange.descriptionRes: Int
    get() = when (this) {
        TimeRange.ONE_MONTH -> R.string.range_one_month_description
        TimeRange.THREE_MONTHS -> R.string.range_three_months_description
        TimeRange.ONE_YEAR -> R.string.range_one_year_description
        TimeRange.ALL -> R.string.range_all_description
    }
