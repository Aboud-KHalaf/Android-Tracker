package com.example.tracker.ui.workout.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.tracker.R

/** Confirms finishing a workout that still has open sets. */
@Composable
fun FinishWorkoutDialog(openSetCount: Int, onFinish: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_finish_title)) },
        text = { Text(pluralStringResource(R.plurals.workout_finish_open_sets, openSetCount, openSetCount)) },
        confirmButton = {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.workout_finish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_keep_training)) }
        },
    )
}

/** Offered instead of finishing when no set is completed: there is nothing to save. */
@Composable
fun DiscardWorkoutDialog(onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_discard_title)) },
        text = { Text(stringResource(R.string.workout_discard_body)) },
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.workout_discard_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_keep_training)) }
        },
    )
}
