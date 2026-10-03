package com.example.tracker.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmReminderSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = AlarmReminderScheduler(context)

    @Test
    fun schedule_keepsOneAlarmAtTheLatestTime_andCancelRemovesIt() {
        scheduler.schedule(Instant.parse("2026-10-03T21:00:00Z"))
        scheduler.schedule(Instant.parse("2026-10-04T21:00:00Z"))

        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(Instant.parse("2026-10-04T21:00:00Z").toEpochMilli(), alarms.nextScheduledAlarm!!.triggerAtTime)

        scheduler.cancel()

        assertNull(alarms.nextScheduledAlarm)
    }
}
