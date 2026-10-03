package com.example.tracker.ui.weight.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.tracker.ui.common.DateTile
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.theme.tabularNumbers
import com.example.tracker.ui.weight.WeightRowUi
import com.example.tracker.ui.weight.weightChangeText
import com.example.tracker.ui.weight.weightText

/** Date tile, "78.4 kg", the weekday and the change since the entry before. Tapping edits it. */
@Composable
fun WeightRow(row: WeightRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(weightText(row.entry.weightKg)) },
        supportingContent = { Text(row.entry.date.dayOfWeek.fullName(currentLocale())) },
        leadingContent = { DateTile(row.entry.date) },
        trailingContent = row.changeKg?.let { change ->
            {
                Text(
                    text = weightChangeText(change),
                    style = MaterialTheme.typography.labelLarge.tabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
