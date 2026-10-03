package com.example.tracker.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.formatClock
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.workout.HoldStatus
import com.example.tracker.ui.workout.SetEditorUi

private const val ADJUST_SECONDS = 5

/** Ticks fast enough that the shown second never lags noticeably. */
private const val TIMER_TICK_MILLIS = 250L

/** Callbacks for timing the active hold. */
data class DurationEditorActions(
    val onToggle: () -> Unit = {},
    val onAdjust: (seconds: Int) -> Unit = {},
)

/** Times a hold: −5s / ring / +5s, then Start-Pause-Resume and Complete. */
@Composable
fun DurationEditor(
    editor: SetEditorUi.Duration,
    actions: DurationEditorActions,
    onComplete: () -> Unit,
) {
    val now = rememberTickingNow(ticking = editor.isRunning, intervalMillis = TIMER_TICK_MILLIS)
    val seconds = editor.secondsAt(now)
    KeepScreenOn(editor.isRunning)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AdjustButton(
                label = stringResource(R.string.hold_minus_label, ADJUST_SECONDS),
                description = pluralStringResource(R.plurals.hold_subtract_description, ADJUST_SECONDS, ADJUST_SECONDS),
                onClick = { actions.onAdjust(-ADJUST_SECONDS) },
            )
            val status = editor.statusAt(now)
            HoldTimerRing(
                timeText = formatClock(seconds),
                statusText = status.text(),
                progress = editor.targetSeconds?.takeIf { it > 0 }?.let { seconds.toFloat() / it } ?: 0f,
                statusColor = if (status is HoldStatus.PastTarget) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            AdjustButton(
                label = stringResource(R.string.hold_plus_label, ADJUST_SECONDS),
                description = pluralStringResource(R.plurals.hold_add_description, ADJUST_SECONDS, ADJUST_SECONDS),
                onClick = { actions.onAdjust(ADJUST_SECONDS) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            val buttonModifier = Modifier
                .weight(1f)
                .height(Dimens.setControlSize)
            FilledTonalButton(onClick = actions.onToggle, modifier = buttonModifier) {
                Icon(if (editor.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = when {
                        editor.isRunning -> stringResource(R.string.hold_pause)
                        seconds > 0 -> stringResource(R.string.hold_resume)
                        else -> stringResource(R.string.hold_start)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Button(onClick = onComplete, enabled = seconds > 0, modifier = buttonModifier) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.hold_complete), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun HoldStatus.text(): String = when (this) {
    HoldStatus.NoTarget -> stringResource(R.string.hold_status_no_target)
    is HoldStatus.BelowTarget -> stringResource(R.string.hold_status_target, formatClock(targetSeconds))
    HoldStatus.MatchedTarget -> stringResource(R.string.hold_status_matched)
    is HoldStatus.PastTarget -> stringResource(R.string.hold_status_past, seconds)
}

@Composable
private fun AdjustButton(label: String, description: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        modifier = Modifier
            .size(Dimens.setControlSize)
            .semantics { contentDescription = description },
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Keeps the display on while a hold is being timed. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
