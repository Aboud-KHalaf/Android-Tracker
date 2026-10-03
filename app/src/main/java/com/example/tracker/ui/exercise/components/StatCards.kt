package com.example.tracker.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.mediumDate
import com.example.tracker.ui.common.shortDate
import com.example.tracker.ui.common.text
import com.example.tracker.ui.exercise.PersonalBestSummaryUi
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.tabularNumbers
import java.time.LocalDate

private val StatIconSize = 32.dp

/** Personal best and Sessions logged, side by side. */
@Composable
fun StatCards(
    personalBest: PersonalBestSummaryUi,
    sessionCount: Int,
    firstSessionOn: LocalDate,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        StatCard(
            icon = Icons.Outlined.EmojiEvents,
            iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
            iconContent = MaterialTheme.colorScheme.onTertiaryContainer,
            label = stringResource(R.string.exercise_personal_best),
            value = personalBest.value.text(),
            detail = personalBest.achievedOn.mediumDate(locale),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Outlined.EventRepeat,
            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
            iconContent = MaterialTheme.colorScheme.onSecondaryContainer,
            label = stringResource(R.string.exercise_sessions_logged),
            value = sessionCount.toString(),
            detail = stringResource(R.string.exercise_since, firstSessionOn.shortDate(locale)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    iconContainer: Color,
    iconContent: Color,
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            IconAvatar(icon, containerColor = iconContainer, contentColor = iconContent, size = StatIconSize)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.tabularNumbers())
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
