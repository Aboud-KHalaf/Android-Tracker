package com.example.tracker.ui.photos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import coil.compose.SubcomposeAsyncImage
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.dateWithYear
import com.example.tracker.ui.photos.labelRes
import com.example.tracker.ui.theme.spacing
import java.io.File

/** Width / height of every progress photo; the camera crops to it too. */
const val PHOTO_ASPECT_RATIO = 3f / 4f

/**
 * A [pose] photo in portrait, or a placeholder naming the missing pose. Clickable only when a
 * photo is shown and [onClick] is set.
 */
@Composable
fun PhotoThumbnail(
    photo: ProgressPhoto?,
    pose: PhotoPose,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val poseName = stringResource(pose.labelRes)
    var container = modifier
        .aspectRatio(PHOTO_ASPECT_RATIO)
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    if (photo != null && onClick != null) {
        container = container.clickable(role = Role.Image, onClick = onClick)
    }
    if (photo == null) {
        Placeholder(
            text = stringResource(R.string.photos_pose_missing, poseName.lowercase(currentLocale())),
            showText = true,
            modifier = container,
        )
    } else {
        SubcomposeAsyncImage(
            model = File(photo.filePath),
            contentDescription = stringResource(R.string.photos_photo_description, poseName, photo.date.dateWithYear(currentLocale())),
            contentScale = ContentScale.Crop,
            loading = { Placeholder(text = poseName, showText = false) },
            // The picture can be missing after restoring a backup, which leaves photos out.
            error = { Placeholder(text = poseName, showText = false, broken = true) },
            modifier = container,
        )
    }
}

@Composable
private fun Placeholder(
    text: String,
    showText: Boolean,
    modifier: Modifier = Modifier,
    broken: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (broken) Icons.Outlined.BrokenImage else Icons.Outlined.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showText) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The day's three photos side by side, in pose order. */
@Composable
fun PhotoRow(
    photos: Map<PhotoPose, ProgressPhoto>,
    onOpenPhoto: (ProgressPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        PhotoPose.entries.forEach { pose ->
            val photo = photos[pose]
            Box(modifier = Modifier.weight(1f)) {
                PhotoThumbnail(photo = photo, pose = pose, onClick = photo?.let { { onOpenPhoto(it) } })
            }
        }
    }
}
