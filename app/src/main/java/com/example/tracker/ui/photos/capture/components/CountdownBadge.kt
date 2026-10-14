package com.example.tracker.ui.photos.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.ui.theme.tabularNumbers

private val BadgeSize = 112.dp

/** The self-timer's seconds left, large enough to read from across the room. */
@Composable
fun CountdownBadge(secondsLeft: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BadgeSize)
            .background(OverlayScrim, CircleShape)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = secondsLeft.toString(),
            style = MaterialTheme.typography.displayLarge.tabularNumbers(),
            color = OverlayContent,
        )
    }
}
