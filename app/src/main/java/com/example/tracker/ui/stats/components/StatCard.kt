package com.example.tracker.ui.stats.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.theme.AccentColors
import com.example.tracker.ui.theme.Motion
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers

/**
 * One stat on the Stats tab: its icon, name, current [value] and a line of context, with an
 * optional [progress] bar (0 to 1) toward a target and a [trailing] action. Tapping opens it;
 * a long press, when [onLongClick] is given, offers more (read out as [onLongClickLabel]).
 * [accessibilityActions] are offered to screen readers in place of gestures they can't make.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatCard(
    title: String,
    icon: ImageVector,
    accent: AccentColors,
    value: String?,
    supporting: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    progressLabel: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .clip(CardDefaults.shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { longClick ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        longClick()
                    }
                },
                onLongClickLabel = onLongClickLabel,
            )
            .semantics { if (accessibilityActions.isNotEmpty()) customActions = accessibilityActions },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
            ) {
                IconAvatar(icon = icon, containerColor = accent.container, contentColor = accent.content)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (value != null) {
                        Text(text = value, style = MaterialTheme.typography.titleLarge.tabularNumbers())
                    }
                    if (supporting != null) {
                        Text(
                            text = supporting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing?.invoke()
            }
            if (progress != null) {
                ProgressLine(progress, progressLabel)
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, label: String?) {
    // Fills from empty when shown, then eases to each new value.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        fill.animateTo(progress, tween(Motion.DurationExtraLong, easing = Motion.Emphasized))
    }
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        LinearProgressIndicator(progress = { fill.value }, modifier = Modifier.fillMaxWidth())
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
