package com.example.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.MuscleGroup

/** "Chest", "Back", … or, for null, "No muscle group". */
@Composable
@ReadOnlyComposable
fun MuscleGroup?.label(): String = stringResource(
    when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.ARMS -> R.string.muscle_arms
        MuscleGroup.LEGS -> R.string.muscle_legs
        MuscleGroup.CORE -> R.string.muscle_core
        null -> R.string.muscle_none
    },
)
