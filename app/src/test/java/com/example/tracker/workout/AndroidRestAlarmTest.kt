package com.example.tracker.workout

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.domain.workout.RestKind
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidRestAlarmTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = shadowOf(context.getSystemService<AlarmManager>())
    private val notifications = shadowOf(context.getSystemService<NotificationManager>())
    private val restAlarm = AndroidRestAlarm(context)
    private val endsAt = Instant.parse("2026-10-03T09:01:30Z")

    /** The app also sets its daily reminder alarm on start; only the rest alarms count here. */
    private fun restAlarms() = alarms.scheduledAlarms.filter {
        shadowOf(it.operation).savedIntent.component?.className == RestEndedReceiver::class.java.name
    }

    @Before
    fun grantNotifications() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun schedule_setsOneAlarmAtTheEndOfTheRest() {
        restAlarm.schedule("w1", RestKind.BETWEEN_SETS, endsAt.minusSeconds(30))
        restAlarm.schedule("w1", RestKind.BETWEEN_SETS, endsAt)

        assertEquals(listOf(endsAt.toEpochMilli()), restAlarms().map { it.triggerAtTime })
    }

    @Test
    fun alarmGoingOff_postsTheRestEndedNotification_andCancelRemovesIt() {
        restAlarm.schedule("w1", RestKind.BETWEEN_EXERCISES, endsAt)
        val alarm = shadowOf(restAlarms().single().operation).savedIntent

        RestEndedReceiver().onReceive(context, alarm)

        val notification = notifications.allNotifications.single()
        assertEquals("Rest's over", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Time for the next exercise", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)

        restAlarm.cancel()
        assertTrue(notifications.allNotifications.isEmpty())
        assertTrue(restAlarms().isEmpty())
    }

    @Test
    fun malformedBroadcast_postsNothing() {
        RestEndedReceiver().onReceive(context, Intent(context, RestEndedReceiver::class.java))

        assertTrue(notifications.allNotifications.isEmpty())
    }
}
