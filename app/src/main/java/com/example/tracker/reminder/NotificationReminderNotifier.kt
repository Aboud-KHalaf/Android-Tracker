package com.example.tracker.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.domain.reminder.ReminderNotifier
import com.example.tracker.ui.nutrition.NUTRITION_DEEP_LINK

/** Posts the reminder notification; tapping it opens the Nutrition tab. */
class NotificationReminderNotifier(private val context: Context) : ReminderNotifier {

    override fun showNutritionReminder() {
        if (!canPostNotifications(context)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.reminder_channel_name))
                .setDescription(context.getString(R.string.reminder_channel_description))
                .build(),
        )
        val openNutrition = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, NUTRITION_DEEP_LINK.toUri(), context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(openNutrition)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post; nothing to show.
        }
    }

    private companion object {
        const val CHANNEL_ID = "daily_reminders"
        const val NOTIFICATION_ID = 1
    }
}

/** Whether the app may post notifications (runtime permission on Android 13+, and not blocked). */
fun canPostNotifications(context: Context): Boolean {
    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}
