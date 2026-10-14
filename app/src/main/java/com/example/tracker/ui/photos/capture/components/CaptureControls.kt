package com.example.tracker.ui.photos.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer10
import androidx.compose.material.icons.outlined.Timer3
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.photos.capture.CaptureTimer
import com.example.tracker.ui.photos.capture.PhotoCaptureUiState
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** The shutter with the timer, last-photo overlay and camera switch around it. */
@Composable
fun CameraControls(
    state: PhotoCaptureUiState,
    canSwitchCamera: Boolean,
    onShutter: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleGuidePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timerRunning = state.countdown != null
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onCycleTimer, enabled = !timerRunning) {
                Icon(state.timer.icon(), contentDescription = state.timer.label())
            }
            if (state.guidePhotoPath != null) {
                IconToggleButton(
                    checked = state.showGuidePhoto,
                    onCheckedChange = { onToggleGuidePhoto() },
                ) {
                    Icon(Icons.Outlined.Layers, contentDescription = stringResource(R.string.capture_overlay_description))
                }
            }
        }
        FilledIconButton(
            onClick = onShutter,
            enabled = state.canShoot || timerRunning,
            modifier = Modifier.size(Dimens.shutterSize),
        ) {
            when {
                state.isTakingPicture -> CircularProgressIndicator(color = LocalContentColor.current, modifier = Modifier.size(24.dp))
                timerRunning -> Icon(Icons.Outlined.Stop, contentDescription = stringResource(R.string.capture_cancel_timer))
                else -> Icon(Icons.Outlined.PhotoCamera, contentDescription = stringResource(R.string.capture_shutter))
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (canSwitchCamera) {
                IconButton(onClick = onSwitchCamera, enabled = !timerRunning) {
                    Icon(Icons.Outlined.Cameraswitch, contentDescription = stringResource(R.string.capture_switch_camera))
                }
            }
        }
    }
}

/** Retake or keep the picture just taken. */
@Composable
fun ReviewControls(
    isSaving: Boolean,
    onRetake: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.shutterSize),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onRetake, enabled = !isSaving, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.capture_retake))
        }
        Button(onClick = onUse, enabled = !isSaving, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.capture_use))
        }
    }
}

private fun CaptureTimer.icon() = when (this) {
    CaptureTimer.OFF -> Icons.Outlined.TimerOff
    CaptureTimer.THREE -> Icons.Outlined.Timer3
    CaptureTimer.TEN -> Icons.Outlined.Timer10
}

@Composable
private fun CaptureTimer.label(): String =
    if (this == CaptureTimer.OFF) stringResource(R.string.capture_timer_off) else stringResource(R.string.capture_timer_seconds, seconds)
