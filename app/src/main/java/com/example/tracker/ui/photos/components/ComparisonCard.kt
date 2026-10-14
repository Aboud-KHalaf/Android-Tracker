package com.example.tracker.ui.photos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.photos.PhotoComparison
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.dateWithYear
import com.example.tracker.ui.photos.labelRes
import com.example.tracker.ui.theme.spacing

/** The first and latest photo of the selected pose side by side. */
@Composable
fun ComparisonCard(
    pose: PhotoPose,
    comparison: PhotoComparison?,
    onSelectPose: (PhotoPose) -> Unit,
    onOpenPhoto: (ProgressPhoto) -> Unit,
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
            Text(
                text = stringResource(R.string.photos_compare),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            PoseSelector(selected = pose, onSelect = onSelectPose)
            if (comparison == null) {
                val poseName = stringResource(pose.labelRes).lowercase(currentLocale())
                Text(
                    text = stringResource(R.string.photos_compare_needs_more, poseName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    LabeledPhoto(stringResource(R.string.photos_before), comparison.before, onOpenPhoto, Modifier.weight(1f))
                    LabeledPhoto(stringResource(R.string.photos_after), comparison.after, onOpenPhoto, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LabeledPhoto(
    label: String,
    photo: ProgressPhoto,
    onOpenPhoto: (ProgressPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        PhotoThumbnail(photo = photo, pose = photo.pose, onClick = { onOpenPhoto(photo) })
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = photo.date.dateWithYear(currentLocale()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
