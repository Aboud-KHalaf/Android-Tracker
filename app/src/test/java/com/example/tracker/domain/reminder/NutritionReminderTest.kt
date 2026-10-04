package com.example.tracker.domain.reminder

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.testing.FakeNutritionRepository
import com.example.tracker.testing.FakeSettingsRepository
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionReminderTest {

    private val settings = FakeSettingsRepository()
    private val nutrition = FakeNutritionRepository()
    private val scheduler = RecordingScheduler()
    private var notifications = 0

    // 21:00 UTC on 3 October; the fake time provider uses UTC.
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T21:00:00Z"))
    private val reminder = NutritionReminder(settings, nutrition, scheduler, { notifications++ }, time)

    @Test
    fun onAlarm_todayNotLogged_notifiesAndSchedulesTomorrow() = runTest {
        reminder.onAlarm()

        assertEquals(1, notifications)
        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun onAlarm_todayLogged_staysQuietButStillSchedulesTomorrow() = runTest {
        nutrition.put(DailyNutrition(LocalDate.of(2026, 10, 3), 2300, 150))

        reminder.onAlarm()

        assertEquals(0, notifications)
        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun onAlarm_disabled_doesNothing() = runTest {
        settings.nutritionReminderEnabled.value = false

        reminder.onAlarm()

        assertEquals(0, notifications)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun onAlarm_readFails_stillSchedulesTomorrow() = runTest {
        val failing = NutritionReminder(settings, BrokenNutrition(), scheduler, { notifications++ }, time)

        runCatching { failing.onAlarm() }

        assertEquals(listOf(Instant.parse("2026-10-04T21:00:00Z")), scheduler.scheduled)
    }

    @Test
    fun sync_schedulesWhenEnabledAndCancelsWhenDisabled() {
        time.current = Instant.parse("2026-10-03T08:00:00Z")
        reminder.sync()

        settings.nutritionReminderEnabled.value = false
        reminder.sync()

        assertEquals(listOf(Instant.parse("2026-10-03T21:00:00Z")), scheduler.scheduled)
        assertEquals(1, scheduler.cancels)
    }

    @Test
    fun sync_usesTheChosenTime() {
        time.current = Instant.parse("2026-10-03T08:00:00Z")
        settings.nutritionReminderTime.value = LocalTime.of(18, 45)

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

    private class BrokenNutrition : com.example.tracker.domain.repository.NutritionRepository by FakeNutritionRepository() {
        override fun observeDay(date: LocalDate) = kotlinx.coroutines.flow.flow<DailyNutrition?> { throw IOException("disk") }
    }
}
