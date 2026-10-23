package com.example.tracker.ui.workout.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.workout.RestKind
import com.example.tracker.ui.common.formatClock
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.workout.RestUi
import java.time.Instant

/** Seconds the −/+ buttons take off or add to a rest. */
internal const val REST_ADJUST_SECONDS = 15

/** Ticks fast enough that the shown second never lags noticeably. */
private const val REST_TICK_MILLIS = 250L

/** Callbacks for the rest countdown. */
data class RestTimerActions(
    val onAdjust: (seconds: Int) -> Unit = {},
    /** Skipped, or the countdown reached zero. */
    val onEnd: () -> Unit = {},
)

/**
 * The current time while [rest] counts down, ticking often enough for its clock. When it
 * runs out, [onEnd] is called, with a haptic buzz if it ran out on screen.
 */
@Composable
fun rememberRestNow(rest: RestUi, onEnd: () -> Unit): Instant {
    val now = rememberTickingNow(intervalMillis = REST_TICK_MILLIS)
    val remaining = rest.remainingSecondsAt(now)
    val haptics = LocalHapticFeedback.current
    // A rest that already ran out while the app was away just ends, without a buzz.
    val endsOnScreen = remember(rest.endsAt) { rest.remainingSecondsAt(Instant.now()) > 0 }
    LaunchedEffect(rest.endsAt, remaining <= 0) {
        if (remaining <= 0) {
            if (endsOnScreen) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onEnd()
        }
    }
    return now
}

/** "Next set in" or "Next exercise in". */
@Composable
fun RestUi.label(): String = stringResource(
    when (kind) {
        RestKind.BETWEEN_SETS -> R.string.rest_between_sets
        RestKind.BETWEEN_EXERCISES -> R.string.rest_before_exercise
    },
)

/**
 * The rest countdown after a completed set, collapsed to a bar above the exercise navigation:
 * time left, −15s / +15s and Skip, over a bar that empties as the rest runs out. Tapping the
 * time opens the full rest screen again ([onExpand]).
 */
@Composable
fun RestTimerBar(rest: RestUi, actions: RestTimerActions, modifier: Modifier = Modifier, onExpand: () -> Unit = {}) {
    val now = rememberRestNow(rest, actions.onEnd)
    val remaining = rest.remainingSecondsAt(now)
    val label = rest.label()
    val timeText = formatClock(remaining)
    val description = stringResource(R.string.rest_remaining_description, label, timeText)
    val expandLabel = stringResource(R.string.rest_expand)
    val spacing = MaterialTheme.spacing

    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) {
        Column {
            LinearProgressIndicator(
                progress = { rest.fractionLeftAt(now) },
                modifier = Modifier.fillMaxWidth(),
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClickLabel = expandLabel, onClick = onExpand)
                        .clearAndSetSemantics {
                            contentDescription = description
                            onClick(label = expandLabel) { onExpand(); true }
                        },
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(timeText, style = MaterialTheme.typography.headlineMedium.tabularNumbers())
                }
                RestAdjustButton(
                    label = stringResource(R.string.hold_minus_label, REST_ADJUST_SECONDS),
                    description = pluralStringResource(
                        R.plurals.rest_subtract_description, REST_ADJUST_SECONDS, REST_ADJUST_SECONDS,
                    ),
                    onClick = { actions.onAdjust(-REST_ADJUST_SECONDS) },
                )
                RestAdjustButton(
                    label = stringResource(R.string.hold_plus_label, REST_ADJUST_SECONDS),
                    description = pluralStringResource(
                        R.plurals.rest_add_description, REST_ADJUST_SECONDS, REST_ADJUST_SECONDS,
                    ),
                    onClick = { actions.onAdjust(REST_ADJUST_SECONDS) },
                )
                TextButton(onClick = actions.onEnd) { Text(stringResource(R.string.rest_skip)) }
            }
        }
    }
}

@Composable
internal fun RestAdjustButton(label: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
