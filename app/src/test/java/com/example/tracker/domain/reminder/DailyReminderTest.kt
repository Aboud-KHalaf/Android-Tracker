package com.example.tracker.domain.reminder

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeSettingsRepository
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyReminderTest {

    private val settings = FakeSettingsRepository()
    private val stats = FakeCustomStatRepository().apply {
        this.stats.value = listOf(stat("weight"), stat("waist"))
    }
    private val scheduler = RecordingScheduler()
    private var notifications = 0

    private val today = LocalDate.of(2026, 10, 3)

    // 21:00 UTC on 3 October; the fake time provider uses UTC.
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T21:00:00Z"))
    private val reminder = DailyReminder(settings, stats, scheduler, { notifications++ }, time)

    @Test
    fun onAlarm_aStatNotLoggedToday_notifiesAndSchedulesTomorrow() = runTest {
        stats.put(StatEntry("weight", today, 78.4))

        reminder.onAlarm()

        assertEquals(1, notifications)
        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun onAlarm_everyStatLoggedToday_staysQuietButStillSchedulesTomorrow() = runTest {
        stats.put(StatEntry("weight", today, 78.4), StatEntry("waist", today, 84.0))

        reminder.onAlarm()

        assertEquals(0, notifications)
        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun onAlarm_withoutStats_staysQuiet() = runTest {
        stats.stats.value = emptyList()

        reminder.onAlarm()

        assertEquals(0, notifications)
        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun onAlarm_disabled_doesNothing() = runTest {
        settings.reminderEnabled.value = false

        reminder.onAlarm()

        assertEquals(0, notifications)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun onAlarm_readFails_stillSchedulesTomorrow() = runTest {
        stats.observeError = IOException("disk")

        runCatching { reminder.onAlarm() }

        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun sync_schedulesWhenEnabledAndCancelsWhenDisabled() {
        time.current = Instant.parse("2026-10-03T08:00:00Z")
        reminder.sync()

        settings.reminderEnabled.value = false
        reminder.sync()

        assertEquals(listOf(Instant.parse("2026-10-03T21:00:00Z")), scheduler.scheduled)
        assertEquals(1, scheduler.cancels)
    }

    @Test
    fun sync_usesTheChosenTime() {
        time.current = Instant.parse("2026-10-03T08:00:00Z")
        settings.reminderTime.value = LocalTime.of(18, 45)

        reminder.sync()

        assertEquals(listOf(Instant.parse("2026-10-03T18:45:00Z")), scheduler.scheduled)
    }

    private class RecordingScheduler : ReminderScheduler {
        val scheduled = mutableListOf<Instant>()
        var cancels = 0
        override fun schedule(at: Instant) {
            scheduled += at
        }
        override fun cancel() {
            cancels++
        }
    }

    private fun stat(id: String) = CustomStat(id, id, "", 1, null, StatIcon.CHART, StatColor.BLUE)
}
