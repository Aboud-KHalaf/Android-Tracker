package com.example.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Returns a function that runs an action, such as starting a workout, after asking for the
 * notification permission the workout's progress notification needs (Android 13+). The
 * action runs either way; without the permission the workout just has no notification.
 */
@Composable
fun rememberAfterNotificationPermission(): (action: () -> Unit) -> Unit {
    // Only lives while the permission dialog is up; not saved, since an action can't be.
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestPermission = rememberNotificationPermissionRequest {
        pending?.invoke()
        pending = null
    }
    return { action ->
        pending = action
        requestPermission()
    }
}
