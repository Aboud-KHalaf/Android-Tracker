package com.example.tracker.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.tracker.reminder.canPostNotifications

/**
 * Returns a function that makes sure the app may post notifications, asking for the Android 13+
 * permission when needed. [onResult] gets whether notifications can be shown afterwards.
 */
@Composable
fun rememberNotificationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onResult(canPostNotifications(context))
    }
    return {
        when {
            canPostNotifications(context) -> onResult(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> onResult(false)
        }
    }
}

/** Opens the Android 12+ "Alarms & reminders" page for this app, where the user can allow exact alarms. */
fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.fromParts("package", packageName, null))
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Opens the system page for this app's notifications, where the user can turn them back on. */
fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", packageName, null))
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
