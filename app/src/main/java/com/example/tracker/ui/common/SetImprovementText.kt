package com.example.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.SetImprovement

/** "+2.5 kg", "+1 rep" or "+5 s". */
@Composable
@ReadOnlyComposable
fun SetImprovement.text(): String = when (this) {
    is SetImprovement.Weight -> stringResource(R.string.improvement_weight, formatWeight(kg, currentLocale()))
    is SetImprovement.Reps -> pluralStringResource(R.plurals.improvement_reps, reps, reps)
    is SetImprovement.Hold -> stringResource(R.string.improvement_hold, seconds)
}
