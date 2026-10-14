package com.example.tracker.ui.weight.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.dateWithYear
import com.example.tracker.ui.photos.components.PhotoRow
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** The latest progress photos with links to all of them and to the camera; or a prompt to start. */
@Composable
fun ProgressPhotosCard(
    latest: PhotoCheckIn?,
    onOpen: () -> Unit,
    onTakePhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.photos_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                if (latest != null) {
                    TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = Dimens.minTouchTarget)) {
                        Text(stringResource(R.string.photos_see_all))
                    }
                }
            }
            if (latest == null) {
                Text(
                    text = stringResource(R.string.photos_card_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.photos_latest, latest.date.dateWithYear(currentLocale())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PhotoRow(photos = latest.photos, onOpenPhoto = { onOpen() })
            }
            FilledTonalButton(onClick = onTakePhotos, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(R.string.photos_take), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
    }
}
