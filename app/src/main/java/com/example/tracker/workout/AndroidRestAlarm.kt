package com.example.tracker.workout

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.domain.workout.RestAlarm
import com.example.tracker.domain.workout.RestKind
import com.example.tracker.reminder.canPostNotifications
import com.example.tracker.reminder.canScheduleExactAlarms
import java.time.Instant

/**
 * Sets an [AlarmManager] alarm for the end of a rest; [RestEndedReceiver] then posts a
 * notification that sounds and vibrates. Tapping it opens the workout. The alarm survives the
 * app being ended in the background. Without exact alarm access, the system may deliver it late.
 */
class AndroidRestAlarm(private val context: Context) : RestAlarm {

    override fun schedule(workoutId: String, kind: RestKind, endsAt: Instant) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        val alarm = alarmIntent(workoutId, kind)
        val triggerAt = endsAt.toEpochMilli()
        if (canScheduleExactAlarms(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarm)
                return
            } catch (ignored: SecurityException) {
                // Exact alarm access was revoked after the check; fall back to an inexact alarm.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarm)
    }

    override fun cancel() {
        context.getSystemService<AlarmManager>()?.cancel(alarmIntent(workoutId = null, kind = null))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Posts "Rest's over"; called by [RestEndedReceiver] when the alarm goes off. */
    fun showRestEnded(workoutId: String, kind: RestKind) {
        if (!canPostNotifications(context)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.rest_channel_name))
                .setDescription(context.getString(R.string.rest_channel_description))
                .build(),
        )
        val text = when (kind) {
            RestKind.BETWEEN_SETS -> R.string.rest_notification_next_set
            RestKind.BETWEEN_EXERCISES -> R.string.rest_notification_next_exercise
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.rest_notification_title))
            .setContentText(context.getString(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openWorkout(workoutId))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (ignored: SecurityException) {
            // Permission revoked between the check and the post; nothing to show.
        }
    }

    /** Only one rest alarm is ever pending: the same request code replaces (or cancels) it. */
    private fun alarmIntent(workoutId: String?, kind: RestKind?): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, RestEndedReceiver::class.java)
            .putExtra(EXTRA_WORKOUT_ID, workoutId)
            .putExtra(EXTRA_REST_KIND, kind?.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openWorkout(workoutId: String): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_REQUEST_CODE,
        Intent(context, MainActivity::class.java)
            .setAction(AndroidActiveWorkoutNotifier.ACTION_OPEN_WORKOUT)
            .putExtra(AndroidActiveWorkoutNotifier.EXTRA_WORKOUT_ID, workoutId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal companion object {
        const val EXTRA_WORKOUT_ID = "com.example.tracker.extra.REST_WORKOUT_ID"
        const val EXTRA_REST_KIND = "com.example.tracker.extra.REST_KIND"
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 3
        // Apart from the app's other pending intents of the same kind.
        private const val ALARM_REQUEST_CODE = 2
        private const val OPEN_REQUEST_CODE = 1
    }
}

/** A rest ended while the app was in the background. */
class RestEndedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workoutId = intent.getStringExtra(AndroidRestAlarm.EXTRA_WORKOUT_ID) ?: return
        val kind = RestKind.entries.firstOrNull { it.name == intent.getStringExtra(AndroidRestAlarm.EXTRA_REST_KIND) }
            ?: return
        AndroidRestAlarm(context.applicationContext).showRestEnded(workoutId, kind)
    }
}
