package com.example.tracker.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.example.tracker.R
import com.example.tracker.ui.common.formatClock
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.workout.RestUi

/** What comes after the rest: an exercise and a line about it, e.g. "Set 3 · Last time 50 kg × 8". */
data class RestUpNext(val title: String, val detail: String?)

/**
 * The rest between sets or exercises, on its own screen: a ring that empties as the rest runs
 * out, −15s / +15s, what comes next and Skip. [onMinimize] shrinks it to a bar so the
 * workout shows again while the rest keeps counting.
 */
@Composable
fun RestScreen(
    rest: RestUi,
    upNext: RestUpNext?,
    actions: RestTimerActions,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberRestNow(rest, actions.onEnd)
    val spacing = MaterialTheme.spacing
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Text(
                text = rest.label(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            HoldTimerRing(
                timeText = formatClock(rest.remainingSecondsAt(now)),
                statusText = stringResource(R.string.rest_of_total, formatClock(rest.totalSeconds)),
                progress = rest.fractionLeftAt(now),
                statusColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                RestAdjustButton(
                    label = stringResource(R.string.hold_minus_label, REST_ADJUST_SECONDS),
                    description = pluralStringResource(
                        R.plurals.rest_subtract_description, REST_ADJUST_SECONDS, REST_ADJUST_SECONDS,
                    ),
                    onClick = { actions.onAdjust(-REST_ADJUST_SECONDS) },
                    modifier = Modifier.height(Dimens.minTouchTarget),
                )
                RestAdjustButton(
                    label = stringResource(R.string.hold_plus_label, REST_ADJUST_SECONDS),
                    description = pluralStringResource(
                        R.plurals.rest_add_description, REST_ADJUST_SECONDS, REST_ADJUST_SECONDS,
                    ),
                    onClick = { actions.onAdjust(REST_ADJUST_SECONDS) },
                    modifier = Modifier.height(Dimens.minTouchTarget),
                )
            }
            upNext?.let { UpNextCard(it) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = actions.onEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.setControlSize),
                ) {
                    Text(stringResource(R.string.rest_skip_rest), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
                TextButton(onClick = onMinimize, modifier = Modifier.padding(top = spacing.sm)) {
                    Icon(Icons.Outlined.CloseFullscreen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.rest_minimize))
                }
            }
        }
    }
}

@Composable
private fun UpNextCard(upNext: RestUpNext) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                stringResource(R.string.rest_up_next),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(upNext.title, style = MaterialTheme.typography.titleLarge)
            upNext.detail?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
