package com.example.tracker.ui.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.formatElapsed
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.time.Instant

private val ProgressSegmentHeight = 4.dp

/** Workout name, clock and position, with Minimize and Finish; progress segments below. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTopBar(
    workoutName: String,
    startedAt: Instant,
    exerciseIndex: Int,
    exerciseCount: Int,
    isFinishing: Boolean,
    onMinimize: () -> Unit,
    onFinish: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.workout_header_subtitle,
                            formatElapsed(rememberElapsedSeconds(startedAt)),
                            exerciseIndex + 1,
                            exerciseCount,
                        ),
                        style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Outlined.ExpandMore, contentDescription = stringResource(R.string.workout_minimize))
                }
            },
            actions = {
                FilledTonalButton(
                    onClick = onFinish,
                    enabled = !isFinishing,
                    modifier = Modifier.padding(end = MaterialTheme.spacing.sm),
                ) { Text(stringResource(R.string.workout_finish)) }
            },
        )
        ExerciseProgress(exerciseIndex, exerciseCount)
    }
}

/** One segment per exercise, filled up to the current one. Decorative: the subtitle says it. */
@Composable
private fun ExerciseProgress(exerciseIndex: Int, exerciseCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.lg)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        repeat(exerciseCount) { index ->
            val color = if (index <= exerciseIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(ProgressSegmentHeight)
                    .background(color, CircleShape),
            )
        }
    }
}
