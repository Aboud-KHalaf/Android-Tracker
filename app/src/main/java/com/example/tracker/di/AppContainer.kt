package com.example.tracker.di

import android.content.Context
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.SystemTimeProvider
import com.example.tracker.core.TimeProvider
import com.example.tracker.core.UuidGenerator
import com.example.tracker.data.export.AndroidReportExporter
import com.example.tracker.data.local.LegacyNutritionTargets
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.remote.UrlConnectionHttpClient
import com.example.tracker.data.remote.wger.WgerApi
import com.example.tracker.data.repository.LocalProgressPhotoRepository
import com.example.tracker.data.repository.LocalSettingsRepository
import com.example.tracker.data.repository.OfflineFirstCustomStatRepository
import com.example.tracker.data.repository.OfflineFirstExerciseRepository
import com.example.tracker.data.repository.OfflineFirstPlanRepository
import com.example.tracker.data.repository.OfflineFirstProgramRepository
import com.example.tracker.data.repository.OfflineFirstWorkoutRepository
import com.example.tracker.data.repository.WgerExerciseCatalogRepository
import com.example.tracker.domain.export.ReportExporter
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.reminder.DailyReminder
import com.example.tracker.domain.repository.ExerciseCatalogRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.domain.repository.ProgressPhotoRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.domain.workout.ActiveWorkoutSession
import com.example.tracker.domain.workout.RestAlarm
import com.example.tracker.reminder.AlarmReminderScheduler
import com.example.tracker.reminder.NotificationReminderNotifier
import com.example.tracker.workout.AndroidActiveWorkoutNotifier
import com.example.tracker.workout.AndroidRestAlarm
import java.io.File
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
    val exerciseCatalogRepository: ExerciseCatalogRepository by lazy {
        WgerExerciseCatalogRepository(WgerApi(UrlConnectionHttpClient()))
    }
    val programRepository: ProgramRepository by lazy { OfflineFirstProgramRepository(database, time, ids) }
    val planRepository: PlanRepository by lazy { OfflineFirstPlanRepository(database, time, ids) }
    val workoutRepository: WorkoutRepository by lazy { OfflineFirstWorkoutRepository(database, time, ids) }
    val customStatRepository: CustomStatRepository by lazy { OfflineFirstCustomStatRepository(database, time, ids) }
    private val photoStore: ProgressPhotoStore by lazy {
        val appContext = context.applicationContext
        ProgressPhotoStore(
            photosDirectory = File(appContext.filesDir, ProgressPhotoStore.PHOTOS_FOLDER),
            capturesDirectory = File(appContext.cacheDir, ProgressPhotoStore.CAPTURES_FOLDER),
        )
    }
    val progressPhotoRepository: ProgressPhotoRepository by lazy {
        LocalProgressPhotoRepository(database, photoStore, time, ids)
    }
    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(LocalSettingsRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    val settingsRepository: SettingsRepository by lazy { LocalSettingsRepository(preferences, database, photoStore) }

    /** Moves the old daily log's targets onto their stats; see [LegacyNutritionTargets]. */
    val legacyNutritionTargets: LegacyNutritionTargets by lazy {
        LegacyNutritionTargets(preferences, database.customStatDao(), time)
    }

    val reportExporter: ReportExporter by lazy { AndroidReportExporter(context.applicationContext) }

    /** For work that outlives a screen, such as handling a broadcast. */
    val applicationScope = CoroutineScope(SupervisorJob())

    val activeWorkoutSession: ActiveWorkoutSession by lazy {
        ActiveWorkoutSession(workoutRepository, AndroidActiveWorkoutNotifier(context.applicationContext))
    }

    val restAlarm: RestAlarm by lazy { AndroidRestAlarm(context.applicationContext) }

    val dailyReminder: DailyReminder by lazy {
        DailyReminder(
            settings = settingsRepository,
            stats = customStatRepository,
            scheduler = AlarmReminderScheduler(context.applicationContext),
            notifier = NotificationReminderNotifier(context.applicationContext),
            time = time,
        )
    }
}
