package com.example.tracker.ui.workout

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.SetType

/** "Set 2" or, for a warm-up, "Warm-up 1". */
@Composable
@ReadOnlyComposable
fun setName(type: SetType, number: Int): String = stringResource(setNameRes(type), number)

@StringRes
fun setNameRes(type: SetType): Int = if (type.isWarmUp) R.string.workout_warm_up_number else R.string.workout_set_number

/** "Normal set", "Warm-up", "Drop set" or "To failure". */
@Composable
@ReadOnlyComposable
fun SetType.label(): String = stringResource(
    when (this) {
        SetType.NORMAL -> R.string.set_type_normal
        SetType.WARM_UP -> R.string.set_type_warm_up
        SetType.DROP -> R.string.set_type_drop
        SetType.FAILURE -> R.string.set_type_failure
    },
)

/** Drop sets and sets to failure carry a tag in the set list; normal sets and warm-ups are named by their label. */
val SetType.hasTag: Boolean get() = this == SetType.DROP || this == SetType.FAILURE
