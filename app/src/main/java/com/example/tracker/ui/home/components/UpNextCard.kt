package com.example.tracker.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.common.mediumDate
import com.example.tracker.ui.home.UpNextUi
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The highlighted card: start the active program's next day, resume a workout, fill an empty
 * program, or create a first program.
 */
@Composable
fun UpNextCard(
    upNext: UpNextUi,
    today: LocalDate,
    isStartingWorkout: Boolean,
    onStartWorkout: (planId: String) -> Unit,
    onResumeWorkout: (workoutId: String) -> Unit,
    onOpenProgram: (programId: String) -> Unit,
    onCreateProgram: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (upNext) {
        is UpNextUi.Start -> UpNextCardLayout(
            label = startLabel(upNext.scheduledOn, today),
            title = upNext.name,
            body = startBody(upNext),
            actionLabel = stringResource(R.string.home_start_workout),
            actionIcon = Icons.Outlined.PlayArrow,
            isBusy = isStartingWorkout,
            onAction = { onStartWorkout(upNext.planId) },
            modifier = modifier,
        )

        is UpNextUi.Resume -> UpNextCardLayout(
            label = stringResource(R.string.home_in_progress),
            title = upNext.name,
            body = stringResource(
                R.string.home_started_at,
                upNext.startedAt.format(
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()),
                ),
            ),
            actionLabel = stringResource(R.string.home_resume_workout),
            actionIcon = Icons.Outlined.PlayArrow,
            isBusy = false,
            onAction = { onResumeWorkout(upNext.workoutId) },
            modifier = modifier,
        )

        is UpNextUi.EmptyProgram -> UpNextCardLayout(
            label = stringResource(R.string.home_active_program),
            title = upNext.name,
            body = stringResource(R.string.home_empty_program_body),
            actionLabel = stringResource(R.string.home_open_program),
            actionIcon = Icons.Outlined.CalendarMonth,
            isBusy = false,
            onAction = { onOpenProgram(upNext.programId) },
            modifier = modifier,
        )

        UpNextUi.NoPrograms -> UpNextCardLayout(
            label = stringResource(R.string.home_get_started),
            title = stringResource(R.string.home_create_first_program),
            body = stringResource(R.string.home_create_first_program_body),
            actionLabel = stringResource(R.string.home_new_program),
            actionIcon = Icons.Outlined.Add,
            isBusy = false,
            onAction = onCreateProgram,
            modifier = modifier,
        )
    }
}

/** "Today", "Next up · Friday", or "Up next" for a day without a fixed weekday. */
@Composable
private fun startLabel(scheduledOn: LocalDate?, today: LocalDate): String = when (scheduledOn) {
    null -> stringResource(R.string.home_up_next)
    today -> stringResource(R.string.home_today)
    else -> stringResource(R.string.home_next_on, scheduledOn.dayOfWeek.fullName(currentLocale()))
}

@Composable
private fun startBody(upNext: UpNextUi.Start): String {
    val exercises = pluralStringResource(R.plurals.exercise_count, upNext.exerciseCount, upNext.exerciseCount)
    val lastDone = upNext.lastDoneOn
        ?.let { stringResource(R.string.home_last_done, it.mediumDate(currentLocale())) }
        ?: stringResource(R.string.home_never_done)
    return exercises + stringResource(R.string.separator_dot) + lastDone
}

@Composable
private fun UpNextCardLayout(
    label: String,
    title: String,
    body: String,
    actionLabel: String,
    actionIcon: ImageVector,
    isBusy: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onAction,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.minTouchTarget),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                }
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(actionLabel)
            }
        }
    }
}
