package com.example.tracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Returns a function that asks for the notification permission the workout's progress
 * notification needs (Android 13+), then calls [onResult] with the [token] it was given, e.g.
 * the plan to start. [onResult] runs either way; without the permission the workout just has
 * no notification. The token is saved, so a rotation during the dialog doesn't lose the tap.
 */
@Composable
fun rememberAfterNotificationPermission(onResult: (token: String) -> Unit): (token: String) -> Unit {
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnResult by rememberUpdatedState(onResult)
    val requestPermission = rememberNotificationPermissionRequest {
        pending?.let(currentOnResult)
        pending = null
    }
    return { token ->
        pending = token
        requestPermission()
    }
}
