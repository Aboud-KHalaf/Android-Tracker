package com.example.tracker.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseType

/** Icon for exercises of this type: a dumbbell for weight × reps, a timer for holds. */
val ExerciseType.icon: ImageVector
    get() = when (this) {
        ExerciseType.WEIGHT_REPS -> Icons.Outlined.FitnessCenter
        ExerciseType.DURATION -> Icons.Outlined.Timer
    }

/** "Weight × reps" or "Duration". */
@Composable
@ReadOnlyComposable
fun ExerciseType.label(): String = when (this) {
    ExerciseType.WEIGHT_REPS -> stringResource(R.string.exercise_type_weight_reps)
    ExerciseType.DURATION -> stringResource(R.string.exercise_type_duration)
}
