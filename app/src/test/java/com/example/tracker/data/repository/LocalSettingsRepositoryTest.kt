package com.example.tracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ThemeMode
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: TrackerDatabase
    private lateinit var photoStore: ProgressPhotoStore
    private lateinit var preferences: SharedPreferences
    private val time = FakeTimeProvider()
    private val ids = SequentialIds()

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        photoStore = ProgressPhotoStore(folder.newFolder("photos"), folder.newFolder("captures"))
        preferences = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("settings-test", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun themeMode_defaultsToSystem() = runTest {
        val settings = LocalSettingsRepository(preferences, database, photoStore, StandardTestDispatcher(testScheduler))

        assertEquals(ThemeMode.SYSTEM, settings.themeMode.value)
    }

    @Test
    fun setThemeMode_updatesStateAndPersists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)

        settings.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settings.themeMode.value)
        assertEquals(ThemeMode.DARK, LocalSettingsRepository(preferences, database, photoStore, dispatcher).themeMode.value)
    }

    @Test
    fun nutritionTargets_defaultToNone() = runTest {
        val settings = LocalSettingsRepository(preferences, database, photoStore, StandardTestDispatcher(testScheduler))

        assertEquals(NutritionTargets(), settings.nutritionTargets.value)
    }

    @Test
    fun setNutritionTargets_updatesStateAndPersists_andNullClearsATarget() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)

        settings.setNutritionTargets(NutritionTargets(calories = 2400, proteinGrams = 160, steps = 8000))
        settings.setNutritionTargets(NutritionTargets(calories = 2200, proteinGrams = null, steps = 10_000))

        assertEquals(NutritionTargets(2200, null, 10_000), settings.nutritionTargets.value)
        assertEquals(NutritionTargets(2200, null, 10_000), LocalSettingsRepository(preferences, database, photoStore, dispatcher).nutritionTargets.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setNutritionTargets_zero_isRejected() = runTest {
        LocalSettingsRepository(preferences, database, photoStore, StandardTestDispatcher(testScheduler))
            .setNutritionTargets(NutritionTargets(calories = 0))
    }

    @Test
    fun nutritionReminder_defaultsOnAndPersistsWhenTurnedOff() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        assertTrue(settings.nutritionReminderEnabled.value)

        settings.setNutritionReminderEnabled(false)

        assertEquals(false, LocalSettingsRepository(preferences, database, photoStore, dispatcher).nutritionReminderEnabled.value)
    }

    @Test
    fun nutritionReminderTime_defaultsToNinePmAndPersists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        assertEquals(LocalTime.of(21, 0), settings.nutritionReminderTime.value)

        settings.setNutritionReminderTime(LocalTime.of(7, 5, 30))

        assertEquals(LocalTime.of(7, 5), LocalSettingsRepository(preferences, database, photoStore, dispatcher).nutritionReminderTime.value)
    }

    @Test
    fun notificationPermissionRequested_persists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        assertEquals(false, LocalSettingsRepository(preferences, database, photoStore, dispatcher).notificationPermissionRequested.value)

        LocalSettingsRepository(preferences, database, photoStore, dispatcher).markNotificationPermissionRequested()

        assertTrue(LocalSettingsRepository(preferences, database, photoStore, dispatcher).notificationPermissionRequested.value)
    }

    @Test
    fun deleteAllData_removesNutritionButKeepsTargets() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        val nutrition = OfflineFirstNutritionRepository(database, time)
        val day = LocalDate.of(2026, 10, 1)
        nutrition.saveDay(DailyNutrition(day, 2300, 150))
        settings.setNutritionTargets(NutritionTargets(2400, 160))

        settings.deleteAllData()

        assertNull(nutrition.observeDay(day).first())
        assertEquals(NutritionTargets(2400, 160), settings.nutritionTargets.value)
    }

    @Test
    fun deleteAllData_removesExercisesPlansAndWorkouts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val exercises = OfflineFirstExerciseRepository(database, time, ids)
        val plans = OfflineFirstPlanRepository(database, time, ids)
        val workouts = OfflineFirstWorkoutRepository(database, time, ids)
        val planId = OfflineFirstProgramRepository(database, time, ids).newDay("Push Day")
        plans.addExercise(planId, exercises.createExercise("Bench Press", ExerciseType.WEIGHT_REPS))
        workouts.startWorkout(planId)

        LocalSettingsRepository(preferences, database, photoStore, dispatcher).deleteAllData()

        assertTrue(exercises.observeExercises().first().isEmpty())
        assertTrue(plans.observePlans().first().isEmpty())
        assertNull(workouts.observeActiveWorkout().first())
    }

    @Test
    fun deleteAllData_removesProgressPhotosAndTheirFiles() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val photos = LocalProgressPhotoRepository(database, photoStore, time, ids, dispatcher)
        val capture = photos.createCaptureFile().apply { writeText("jpeg") }
        photos.savePhoto(LocalDate.of(2026, 10, 1), PhotoPose.FRONT, capture)
        val saved = photos.observePhotos().first().single()

        LocalSettingsRepository(preferences, database, photoStore, dispatcher).deleteAllData()

        assertTrue(photos.observePhotos().first().isEmpty())
        assertFalse(java.io.File(saved.filePath).exists())
    }
}
