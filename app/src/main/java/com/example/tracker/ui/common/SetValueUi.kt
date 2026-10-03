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

/**
 * A list of sets in short form: "45×10 · 50×8 · 50×8 kg" for weights, "0:45 · 0:40" for holds.
 * Assumes all sets are of one kind, as they are within an exercise.
 */
@Composable
@ReadOnlyComposable
fun List<SetValueUi>.compactText(): String {
    val locale = currentLocale()
    val separator = stringResource(R.string.separator_dot)
    val parts = map { value ->
        when (value) {
            is SetValueUi.WeightReps ->
                stringResource(R.string.set_weight_reps_compact, formatWeight(value.weightKg, locale), value.reps)
            is SetValueUi.Hold -> formatClock(value.seconds)
        }
    }
    val joined = parts.joinToString(separator)
    return if (firstOrNull() is SetValueUi.WeightReps) stringResource(R.string.workout_last_time_weights, joined) else joined
}
