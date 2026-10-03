package com.example.tracker.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.longDate
import com.example.tracker.ui.theme.spacing
import java.time.LocalDate

/** "Saturday, October 3" over the "Today" title, with the Settings button on the right. */
@Composable
fun HomeHeader(today: LocalDate, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                text = today.longDate(currentLocale()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_open))
        }
    }
}
