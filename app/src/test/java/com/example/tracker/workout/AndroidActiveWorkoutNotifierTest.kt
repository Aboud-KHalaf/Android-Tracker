package com.example.tracker.workout

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.domain.workout.ActiveWorkoutStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidActiveWorkoutNotifierTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = shadowOf(context.getSystemService<NotificationManager>())
    private val notifier = AndroidActiveWorkoutNotifier(context)
    private val started = Instant.parse("2026-10-03T08:30:00Z")
    private val status = ActiveWorkoutStatus("w1", "Push", started, "Bench Press", completedSets = 4, totalSets = 12)

    @Test
    fun show_postsAnOngoingNotificationWithAClockFromTheStart() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(status)

        val notification = manager.allNotifications.single()
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(started.toEpochMilli(), notification.`when`)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals("Push", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Bench Press · 4 of 12 sets done", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(Notification.CATEGORY_WORKOUT, notification.category)
    }

    @Test
    fun showingAgain_updatesTheSameNotification_andCancelRemovesIt() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(status)
        notifier.show(status.copy(completedSets = 5))
        assertEquals(1, manager.allNotifications.size)

        notifier.cancel()
        assertTrue(manager.allNotifications.isEmpty())
    }

    @Test
    fun withoutPermission_nothingIsPosted() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(status)

        assertTrue(manager.allNotifications.isEmpty())
    }
}
