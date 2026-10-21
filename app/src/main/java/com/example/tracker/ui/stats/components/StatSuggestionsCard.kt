package com.example.tracker.ui.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.ui.stats.StatTemplate
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.theme.spacing

/**
 * Ready-made stats to start from, leaving out those the user already has ([existingNames],
 * compared ignoring case). Before the first stat it also explains what stats are for.
 * Shows nothing once every suggestion is taken.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatSuggestionsCard(
    existingNames: Set<String>,
    isFirstStat: Boolean,
    onPick: (StatTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val taken = existingNames.mapTo(mutableSetOf()) { it.trim().lowercase() }
    val suggestions = StatTemplate.entries.map { it to stringResource(it.nameRes) }
        .filterNot { (_, name) -> name.lowercase() in taken }
    if (suggestions.isEmpty()) return
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(if (isFirstStat) R.string.stats_empty_title else R.string.stats_suggestions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (isFirstStat) {
                Text(
                    text = stringResource(R.string.stats_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                suggestions.forEach { (template, name) ->
                    AssistChip(
                        onClick = { onPick(template) },
                        label = { Text(name) },
                        leadingIcon = {
                            Icon(
                                template.icon.imageVector,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
    }
}
