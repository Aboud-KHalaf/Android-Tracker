package com.example.tracker.ui.plan.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.plan.WorkoutActionUi

private val ProgressSize = 24.dp

/** "Start workout" or "Resume workout"; nothing when the plan can't be started. */
@Composable
fun WorkoutFab(
    action: WorkoutActionUi,
    isStartingWorkout: Boolean,
    onStart: () -> Unit,
    onResume: (workoutId: String) -> Unit,
) {
    val label = when (action) {
        WorkoutActionUi.Start -> stringResource(R.string.plan_start_workout)
        is WorkoutActionUi.Resume -> stringResource(R.string.plan_resume_workout)
        WorkoutActionUi.Unavailable -> return
    }
    ExtendedFloatingActionButton(
        text = { Text(label) },
        icon = {
            if (isStartingWorkout) {
                CircularProgressIndicator(modifier = Modifier.size(ProgressSize), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            }
        },
        onClick = {
            when (action) {
                WorkoutActionUi.Start -> if (!isStartingWorkout) onStart()
                is WorkoutActionUi.Resume -> onResume(action.workoutId)
                WorkoutActionUi.Unavailable -> Unit
            }
        },
    )
}
