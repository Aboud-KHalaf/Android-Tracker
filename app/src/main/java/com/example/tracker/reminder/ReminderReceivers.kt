package com.example.tracker.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tracker.TrackerApplication
import kotlinx.coroutines.launch

/** The reminder alarm went off. */
class NutritionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as TrackerApplication).container
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                container.nutritionReminder.onAlarm()
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Alarms are lost on reboot and fire at the wrong moment after a time or time zone change, so
 * the next reminder is scheduled again on those events and after an app update. It is also
 * rescheduled when exact alarm access is granted, so the pending alarm becomes exact.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ACTIONS) {
            (context.applicationContext as TrackerApplication).container.nutritionReminder.sync()
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
