package com.example.tracker.ui.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.ui.common.currentLocale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

val StatIcon.imageVector: ImageVector
    get() = when (this) {
        StatIcon.CHART -> Icons.AutoMirrored.Outlined.ShowChart
        StatIcon.SCALE -> Icons.Outlined.MonitorWeight
        StatIcon.RULER -> Icons.Outlined.Straighten
        StatIcon.PERCENT -> Icons.Outlined.Percent
        StatIcon.FIRE -> Icons.Outlined.LocalFireDepartment
        StatIcon.WATER -> Icons.Outlined.WaterDrop
        StatIcon.SLEEP -> Icons.Outlined.Bedtime
        StatIcon.HEART -> Icons.Outlined.FavoriteBorder
        StatIcon.BOLT -> Icons.Outlined.Bolt
        StatIcon.STEPS -> Icons.AutoMirrored.Outlined.DirectionsWalk
        StatIcon.FOOD -> Icons.Outlined.Restaurant
        StatIcon.STAR -> Icons.Outlined.StarOutline
    }

/** "1,250", "19.5" or "19.50": grouped, always with [decimals] places. */
fun formatStatNumber(value: Double, decimals: Int, locale: Locale): String {
    val pattern = if (decimals <= 0) "#,##0" else "#,##0." + "0".repeat(decimals)
    return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(value)
}

/** "19.5 %", "84.0 cm", or just "7" for a stat without a unit. */
@Composable
@ReadOnlyComposable
fun statValueText(value: Double, stat: CustomStat): String =
    withUnit(formatStatNumber(value, stat.decimals, currentLocale()), stat.unit)

/** "+0.4 cm", "−1.2 cm" or "0.0 cm", at the stat's decimals. */
@Composable
@ReadOnlyComposable
fun statChangeText(change: Double, stat: CustomStat): String {
    val rounded = roundTo(change, stat.decimals)
    val amount = withUnit(formatStatNumber(abs(rounded), stat.decimals, currentLocale()), stat.unit)
    return when {
        rounded > 0 -> stringResource(R.string.stat_change_up, amount)
        rounded < 0 -> stringResource(R.string.stat_change_down, amount)
        else -> amount
    }
}

@Composable
@ReadOnlyComposable
private fun withUnit(number: String, unit: String): String =
    if (unit.isBlank()) number else stringResource(R.string.stat_value_with_unit, number, unit)

/** [value] rounded half-up to [decimals] places, which also hides float noise like 19.499999. */
fun roundTo(value: Double, decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (value * factor).roundToLong() / factor
}

private const val MAX_INPUT_LENGTH = 10

/**
 * Keeps what can be part of a number with up to [decimals] places, e.g. "-12.5":
 * digits, a leading minus, and one decimal separator (a comma becomes a dot).
 */
fun sanitizeStatInput(text: String, decimals: Int): String {
    val negative = text.trimStart().startsWith('-')
    val normalized = text.replace(',', '.').filter { it.isDigit() || (it == '.' && decimals > 0) }
    val dot = normalized.indexOf('.')
    val cleaned = if (dot < 0) {
        normalized
    } else {
        normalized.substring(0, dot + 1) + normalized.substring(dot + 1).replace(".", "").take(decimals)
    }
    return ((if (negative) "-" else "") + cleaned).take(MAX_INPUT_LENGTH)
}

/** The number in [text] if it is a valid stat value, otherwise null. */
fun parseStatInput(text: String): Double? =
    text.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it in CustomStat.MIN_VALUE..CustomStat.MAX_VALUE }

/** "19.5" for a field: the value at [decimals] places, without grouping. */
fun Double.toStatInput(decimals: Int): String =
    if (decimals <= 0) roundTo(this, 0).toLong().toString() else "%.${decimals}f".format(Locale.ROOT, roundTo(this, decimals))
