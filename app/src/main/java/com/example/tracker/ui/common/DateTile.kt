package com.example.tracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tracker.ui.theme.tabularNumbers
import java.time.LocalDate

private val TileSize = 48.dp

/** Month and day in a small tile ("OCT" over "2"), leading workout rows. */
@Composable
fun DateTile(date: LocalDate, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    Column(
        modifier = modifier
            .size(TileSize)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.shortMonth(locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
        )
    }
}
