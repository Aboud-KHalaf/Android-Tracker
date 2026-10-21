package com.example.tracker.ui.settings.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import com.example.tracker.R
import com.example.tracker.domain.health.HealthAvailability
import com.example.tracker.ui.settings.HealthUi
import com.example.tracker.ui.theme.spacing
import java.time.Instant

/** What the Health Connect section can do. */
data class HealthConnectActions(
    val permissions: Set<String> = emptySet(),
    val onPermissionsResult: (granted: Set<String>) -> Unit = {},
    val onDisable: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
)

/**
 * Health Connect sync: a switch that asks for read access, then "Sync now" with when it last
 * worked. When Health Connect still has to be installed or updated, a link to get it instead.
 * Only shown on phones that can have Health Connect, so its classes are only used there.
 */
@Composable
fun HealthConnectSection(health: HealthUi, actions: HealthConnectActions) {
    when (health.availability) {
        HealthAvailability.UNSUPPORTED -> Unit
        HealthAvailability.NEEDS_INSTALL -> {
            val context = LocalContext.current
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_health_install)) },
                supportingContent = { Text(stringResource(R.string.settings_health_install_description)) },
                leadingContent = { Icon(Icons.Outlined.GetApp, contentDescription = null) },
                modifier = Modifier.clickable { context.openHealthConnectInStore() },
            )
        }
        HealthAvailability.AVAILABLE -> AvailableHealthConnect(health, actions)
    }
}

@Composable
private fun AvailableHealthConnect(health: HealthUi, actions: HealthConnectActions) {
    val onResult by rememberUpdatedState(actions.onPermissionsResult)
    val requestPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> onResult(granted) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_health_sync)) },
        supportingContent = { Text(stringResource(R.string.settings_health_sync_description)) },
        leadingContent = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
        trailingContent = { Switch(checked = health.isEnabled, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = health.isEnabled, role = Role.Switch) { enable ->
            if (enable) requestPermissions.launch(actions.permissions) else actions.onDisable()
        },
    )
    if (health.isEnabled) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_health_sync_now)) },
            supportingContent = { Text(lastSyncText(health.lastSync)) },
            leadingContent = { Icon(Icons.Outlined.Sync, contentDescription = null) },
            trailingContent = if (health.isSyncing) {
                { CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.spacing.xl)) }
            } else {
                null
            },
            modifier = Modifier.clickable(enabled = !health.isSyncing, onClick = actions.onSyncNow),
        )
    }
}

/** "Last synced 5 minutes ago", or "Not synced yet". */
@Composable
private fun lastSyncText(lastSync: Instant?): String {
    if (lastSync == null) return stringResource(R.string.settings_health_never_synced)
    val relative = DateUtils.getRelativeTimeSpanString(
        lastSync.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    return stringResource(R.string.settings_health_last_sync, relative)
}

private fun Context.openHealthConnectInStore() {
    val uri = "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending"))
    } catch (_: ActivityNotFoundException) {
        // No Play Store: there's nowhere to get it from.
    }
}

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
