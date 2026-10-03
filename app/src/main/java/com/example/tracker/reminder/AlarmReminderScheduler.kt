package com.example.tracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.example.tracker.domain.reminder.ReminderScheduler
import java.time.Instant

/**
 * Schedules the reminder with [AlarmManager]. The alarm is inexact but allowed while the device
 * dozes, so it needs no exact-alarm permission and may arrive a few minutes late.
 */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager: AlarmManager? get() = context.getSystemService()

    override fun schedule(at: Instant) {
        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent())
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
