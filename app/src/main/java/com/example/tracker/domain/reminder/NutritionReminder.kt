package com.example.tracker.domain.reminder

import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.repository.NutritionRepository
import com.example.tracker.domain.repository.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.first

/** Sets or clears the platform alarm for the next reminder. */
interface ReminderScheduler {
    fun schedule(at: Instant)
    fun cancel()
}

/** Shows the "log today" notification. */
fun interface ReminderNotifier {
    fun showNutritionReminder()
}

/**
 * The daily nutrition reminder: at [ReminderTime.AT], if today has no entry, the user gets a
 * notification. Only one alarm is ever pending; each one schedules the next.
 */
class NutritionReminder(
    private val settings: SettingsRepository,
    private val nutrition: NutritionRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
    private val time: TimeProvider,
) {
    /** Schedules the next reminder if it is enabled, otherwise cancels it. Safe to call any time. */
    fun sync() {
        if (settings.nutritionReminderEnabled.value) {
            scheduler.schedule(ReminderTime.nextAfter(time.now().atZone(time.zone())).toInstant())
        } else {
            scheduler.cancel()
        }
    }

    /** The alarm went off: remind the user if today isn't logged, then set tomorrow's alarm. */
    suspend fun onAlarm() {
        if (!settings.nutritionReminderEnabled.value) return
        try {
            val today = time.now().atZone(time.zone()).toLocalDate()
            if (nutrition.observeDay(today).first() == null) notifier.showNutritionReminder()
        } finally {
            sync()
        }
    }
}
