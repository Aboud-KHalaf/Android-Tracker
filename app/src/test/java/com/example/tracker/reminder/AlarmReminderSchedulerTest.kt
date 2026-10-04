package com.example.tracker.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmReminderSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = AlarmReminderScheduler(context)

    @After
    fun resetExactAlarmAccess() {
        ShadowAlarmManager.reset()
    }

    @Test
    fun schedule_keepsOneAlarmAtTheLatestTime_andCancelRemovesIt() {
        scheduler.schedule(Instant.parse("2026-10-03T21:00:00Z"))
        scheduler.schedule(Instant.parse("2026-10-04T21:00:00Z"))

        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(Instant.parse("2026-10-04T21:00:00Z").toEpochMilli(), alarms.nextScheduledAlarm!!.triggerAtTime)

        scheduler.cancel()

        assertNull(alarms.nextScheduledAlarm)
    }

    @Test
    fun schedule_exactAlarmsAllowed_setsAnExactAlarmThatFiresWhileIdle() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.schedule(Instant.parse("2026-10-03T21:00:00Z"))

        val alarm = alarms.nextScheduledAlarm!!
        assertTrue(alarm.isAllowWhileIdle)
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, alarm.windowLengthMs)
    }

    @Test
    fun schedule_exactAlarmsDenied_stillSetsAnAlarmThatFiresWhileIdle() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.schedule(Instant.parse("2026-10-03T21:00:00Z"))

        val alarm = alarms.nextScheduledAlarm!!
        assertFalse(canScheduleExactAlarms(context))
        assertTrue(alarm.isAllowWhileIdle)
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, alarm.windowLengthMs)
    }
}
