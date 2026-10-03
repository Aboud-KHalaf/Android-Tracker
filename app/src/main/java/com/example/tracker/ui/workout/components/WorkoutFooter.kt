package com.example.tracker.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.tracker.R
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** Previous exercise, and Next (or Finish workout on the last exercise). */
@Composable
fun WorkoutFooter(
    hasPrevious: Boolean,
    nextExerciseName: String?,
    isFinishing: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(
                    start = MaterialTheme.spacing.lg,
                    end = MaterialTheme.spacing.lg,
                    top = MaterialTheme.spacing.md,
                    bottom = MaterialTheme.spacing.lg,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            OutlinedIconButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.size(Dimens.setControlSize),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.workout_previous_exercise),
                )
            }
            val buttonModifier = Modifier
                .weight(1f)
                .height(Dimens.setControlSize)
            if (nextExerciseName != null) {
                FilledTonalButton(onClick = onNext, modifier = buttonModifier) {
                    Text(
                        text = stringResource(R.string.workout_next_exercise, nextExerciseName),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            } else {
                Button(onClick = onFinish, enabled = !isFinishing, modifier = buttonModifier) {
                    Icon(Icons.Outlined.Flag, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.workout_finish_workout))
                }
            }
        }
    }
}
