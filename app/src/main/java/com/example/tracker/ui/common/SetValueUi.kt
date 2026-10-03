package com.example.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.tracker.R

/** A set's result, as shown in summaries. */
sealed interface SetValueUi {
    data class WeightReps(val weightKg: Double, val reps: Int) : SetValueUi
    data class Hold(val seconds: Int) : SetValueUi
}

/** "50 kg × 8" or "1:05". */
@Composable
@ReadOnlyComposable
fun SetValueUi.text(): String = when (this) {
    is SetValueUi.WeightReps -> stringResource(R.string.set_weight_reps, formatWeight(weightKg, currentLocale()), reps)
    is SetValueUi.Hold -> formatClock(seconds)
}
