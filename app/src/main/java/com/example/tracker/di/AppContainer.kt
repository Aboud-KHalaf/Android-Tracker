package com.example.tracker.di

import android.content.Context
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.SystemTimeProvider
import com.example.tracker.core.TimeProvider
import com.example.tracker.core.UuidGenerator
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.repository.LocalSettingsRepository
import com.example.tracker.data.repository.OfflineFirstExerciseRepository
import com.example.tracker.data.repository.OfflineFirstNutritionRepository
import com.example.tracker.data.repository.OfflineFirstPlanRepository
import com.example.tracker.data.repository.OfflineFirstWorkoutRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.NutritionRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.domain.reminder.NutritionReminder
import com.example.tracker.reminder.AlarmReminderScheduler
import com.example.tracker.reminder.NotificationReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * App-wide dependencies, created once in [com.example.tracker.TrackerApplication].
 * ViewModels receive repositories from here through their factories.
 */
class AppContainer(context: Context) {
    val time: TimeProvider = SystemTimeProvider
    private val ids: IdGenerator = UuidGenerator
    private val database: TrackerDatabase by lazy { TrackerDatabase.create(context.applicationContext) }

    val exerciseRepository: ExerciseRepository by lazy { OfflineFirstExerciseRepository(database, time, ids) }
    val planRepository: PlanRepository by lazy { OfflineFirstPlanRepository(database, time, ids) }
    val workoutRepository: WorkoutRepository by lazy { OfflineFirstWorkoutRepository(database, time, ids) }
    val nutritionRepository: NutritionRepository by lazy { OfflineFirstNutritionRepository(database, time) }
    val settingsRepository: SettingsRepository by lazy {
        val preferences = context.applicationContext
            .getSharedPreferences(LocalSettingsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
        LocalSettingsRepository(preferences, database)
    }

    /** For work that outlives a screen, such as handling a broadcast. */
    val applicationScope = CoroutineScope(SupervisorJob())

    val nutritionReminder: NutritionReminder by lazy {
        NutritionReminder(
            settings = settingsRepository,
            nutrition = nutritionRepository,
            scheduler = AlarmReminderScheduler(context.applicationContext),
            notifier = NotificationReminderNotifier(context.applicationContext),
            time = time,
        )
    }
}
