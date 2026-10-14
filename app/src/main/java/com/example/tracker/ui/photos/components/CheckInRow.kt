package com.example.tracker.ui.photos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.domain.photos.PhotoCheckIn
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.dateWithYear
import com.example.tracker.ui.theme.spacing

/** One day's date and its front, side and back photos. */
@Composable
fun CheckInRow(
    checkIn: PhotoCheckIn,
    onOpenPhoto: (ProgressPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(text = checkIn.date.dateWithYear(currentLocale()), style = MaterialTheme.typography.titleSmall)
        PhotoRow(photos = checkIn.photos, onOpenPhoto = onOpenPhoto)
    }
}
