package com.example.tracker.domain.reminder

import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.first

/** Sets or clears the platform alarm for the next reminder. */
interface ReminderScheduler {
    fun schedule(at: Instant)
    fun cancel()
}

/** Shows the "log your stats" notification. */
fun interface ReminderNotifier {
    fun showStatsReminder()
}

/**
 * The daily reminder: at the time the user picked, if a stat isn't logged today, the user gets
 * a notification. Without any stats there is nothing to remind about. Only one alarm is ever
 * pending; each one schedules the next.
 */
class DailyReminder(
    private val settings: SettingsRepository,
    private val stats: CustomStatRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
    private val time: TimeProvider,
) {
    /** Schedules the next reminder if it is enabled, otherwise cancels it. Safe to call any time. */
    fun sync() {
        if (settings.reminderEnabled.value) {
            val next = ReminderTime.nextAfter(time.now().atZone(time.zone()), settings.reminderTime.value)
            scheduler.schedule(next.toInstant())
        } else {
            scheduler.cancel()
        }
    }

    /** The alarm went off: remind the user if a stat isn't logged today, then set tomorrow's alarm. */
    suspend fun onAlarm() {
        if (!settings.reminderEnabled.value) return
        try {
            val today = time.now().atZone(time.zone()).toLocalDate()
            if (stats.observeOverviews().first().any { it.latest?.date != today }) notifier.showStatsReminder()
        } finally {
            sync()
        }
    }
}
