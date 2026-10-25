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
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.photos.capture.CaptureTimer
import com.example.tracker.ui.photos.capture.PhotoCaptureUiState
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** The shutter with the timer, last-photo overlay, gallery and camera switch around it. */
@Composable
fun CameraControls(
    state: PhotoCaptureUiState,
    canSwitchCamera: Boolean,
    onShutter: () -> Unit,
    onSelectTimer: (CaptureTimer) -> Unit,
    onToggleGuidePhoto: () -> Unit,
    onSwitchCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timerRunning = state.countdown != null
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Start) {
            TimerMenu(selected = state.timer, enabled = !timerRunning, onSelect = onSelectTimer)
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
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onPickFromGallery, enabled = state.canShoot) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = stringResource(R.string.capture_from_gallery))
            }
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

/** The self-timer's length ("Off", "3 s", …); tapping offers every length to pick from. */
@Composable
private fun TimerMenu(selected: CaptureTimer, enabled: Boolean, onSelect: (CaptureTimer) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.capture_timer_description, selected.label())
    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
            modifier = Modifier
                .heightIn(min = Dimens.minTouchTarget)
                .semantics { contentDescription = description },
        ) {
            Icon(selected.icon(), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(selected.shortLabel())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CaptureTimer.entries.forEach { timer ->
                DropdownMenuItem(
                    text = { Text(timer.label()) },
                    leadingIcon = { Icon(timer.icon(), contentDescription = null) },
                    trailingIcon = if (timer == selected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(timer)
                    },
                )
            }
        }
    }
}

private fun CaptureTimer.icon() = if (this == CaptureTimer.OFF) Icons.Outlined.TimerOff else Icons.Outlined.Timer

/** "Timer off" or "5 s timer". */
@Composable
private fun CaptureTimer.label(): String =
    if (this == CaptureTimer.OFF) stringResource(R.string.capture_timer_off) else stringResource(R.string.capture_timer_seconds, seconds)

/** "Off" or "5 s", short enough to sit beside the shutter. */
@Composable
private fun CaptureTimer.shortLabel(): String = if (this == CaptureTimer.OFF) {
    stringResource(R.string.capture_timer_off_short)
} else {
    stringResource(R.string.capture_timer_seconds_short, seconds)
}
