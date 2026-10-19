package com.example.tracker.ui.stats.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.ui.common.DateTile
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.stats.detail.StatRowUi
import com.example.tracker.ui.stats.statChangeText
import com.example.tracker.ui.stats.statValueText
import com.example.tracker.ui.theme.tabularNumbers

/** Date tile, "19.5 %", the weekday and the change since the entry before. Tapping edits it. */
@Composable
fun StatRow(stat: CustomStat, row: StatRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(statValueText(row.entry.value, stat)) },
        supportingContent = { Text(row.entry.date.dayOfWeek.fullName(currentLocale())) },
        leadingContent = { DateTile(row.entry.date) },
        trailingContent = row.change?.let { change ->
            {
                Text(
                    text = statChangeText(change, stat),
                    style = MaterialTheme.typography.labelLarge.tabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
