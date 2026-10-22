package com.example.tracker.workout

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.domain.workout.ActiveWorkoutNotifier
import com.example.tracker.domain.workout.ActiveWorkoutStatus
import com.example.tracker.reminder.canPostNotifications

/**
 * The ongoing "workout in progress" notification. Its clock is a system chronometer counting
 * from the workout's start time, so it stays right while the app is in the background or not
 * running at all. Tapping it opens the workout.
 *
 * No foreground service is used: the app doesn't need to run while the user is away, because
 * every set is saved as it is logged. The notification only shows what is stored.
 */
class AndroidActiveWorkoutNotifier(private val context: Context) : ActiveWorkoutNotifier {

    override fun show(status: ActiveWorkoutStatus) {
        if (!canPostNotifications(context)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.workout_channel_name))
                .setDescription(context.getString(R.string.workout_channel_description))
                .setShowBadge(false)
                .build(),
        )
        val progress = context.resources.getQuantityString(
            R.plurals.workout_notification_sets,
            status.totalSets,
            status.completedSets,
            status.totalSets,
        )
        val text = status.currentExerciseName
            ?.let { context.getString(R.string.workout_notification_text, it, progress) }
            ?: progress
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(status.name)
            .setContentText(text)
            .setWhen(status.startedAt.toEpochMilli())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openWorkout(status.workoutId))
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (ignored: SecurityException) {
            // Permission revoked between the check and the post; nothing to show.
        }
    }

    override fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Brings the app's task forward (or starts it) and opens the workout. */
    private fun openWorkout(workoutId: String): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_WORKOUT)
            .putExtra(EXTRA_WORKOUT_ID, workoutId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.example.tracker.action.OPEN_WORKOUT"
        const val EXTRA_WORKOUT_ID = "com.example.tracker.extra.WORKOUT_ID"
        private const val CHANNEL_ID = "active_workout"
        private const val NOTIFICATION_ID = 2
    }
}
