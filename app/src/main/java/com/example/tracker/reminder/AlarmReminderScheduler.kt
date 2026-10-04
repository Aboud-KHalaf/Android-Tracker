package com.example.tracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.example.tracker.domain.reminder.ReminderScheduler
import java.time.Instant

/**
 * Schedules the reminder with [AlarmManager]. Both kinds of alarm fire while the device dozes
 * and wake it up. When exact alarms are allowed the reminder arrives on time; otherwise the
 * system may deliver it up to about an hour late.
 */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager: AlarmManager? get() = context.getSystemService()

    override fun schedule(at: Instant) {
        val manager = alarmManager ?: return
        val triggerAt = at.toEpochMilli()
        if (manager.canScheduleExactAlarmsCompat()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
                return
            } catch (e: SecurityException) {
                // Exact alarm access was revoked after the check; fall back to an inexact alarm.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
    }

    override fun cancel() {
        alarmManager?.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, NutritionReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 1
    }
}

/** Whether the app may set exact alarms (Android 12+ asks for "Alarms & reminders" access). */
fun canScheduleExactAlarms(context: Context): Boolean =
    context.getSystemService<AlarmManager>()?.canScheduleExactAlarmsCompat() ?: false

private fun AlarmManager.canScheduleExactAlarmsCompat(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()
