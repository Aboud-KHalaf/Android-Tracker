package com.example.tracker.workout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tracker.TrackerApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Notifications are cleared on reboot and when the app is updated. If a workout was still in
 * progress, this puts its notification back; the workout itself was never lost, since it is
 * stored in the database.
 */
class ActiveWorkoutRestoreReceiver : BroadcastReceiver() {
    // A broadcast must never crash the app, whatever restoring the notification throws.
    @Suppress("TooGenericExceptionCaught")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTORE_ACTIONS) return
        val container = (context.applicationContext as TrackerApplication).container
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                container.activeWorkoutSession.syncNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The workout is still stored and reopens with the app; only the notification is missing.
                Log.w(TAG, "Couldn't restore the active workout notification", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ActiveWorkoutRestore"
        val RESTORE_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
