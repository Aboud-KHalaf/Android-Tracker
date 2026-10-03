package com.example.tracker.ui.weight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.formatWeight
import kotlin.math.abs
import kotlin.math.roundToInt

/** "78.4 kg". */
@Composable
@ReadOnlyComposable
fun weightText(kg: Double): String = stringResource(R.string.weight_kg, formatWeight(kg, currentLocale()))

/** "+0.4 kg", "−1.2 kg" or "0 kg", rounded to one decimal. */
@Composable
@ReadOnlyComposable
fun weightChangeText(changeKg: Double): String {
    val tenths = (changeKg * 10).roundToInt()
    val amount = formatWeight(abs(tenths) / 10.0, currentLocale())
    return stringResource(
        when {
            tenths > 0 -> R.string.weight_change_up
            tenths < 0 -> R.string.weight_change_down
            else -> R.string.weight_kg
        },
        amount,
    )
}
