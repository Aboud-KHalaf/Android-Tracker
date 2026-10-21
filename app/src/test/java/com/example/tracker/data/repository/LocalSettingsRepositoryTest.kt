package com.example.tracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.tracker.data.local.ProgressPhotoStore
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.workout.RestTimes
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
    fun reminder_defaultsOnAndPersistsWhenTurnedOff() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        assertTrue(settings.reminderEnabled.value)

        settings.setReminderEnabled(false)

        assertEquals(false, LocalSettingsRepository(preferences, database, photoStore, dispatcher).reminderEnabled.value)
    }

    @Test
    fun reminderTime_defaultsToNinePmAndPersists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        assertEquals(LocalTime.of(21, 0), settings.reminderTime.value)

        settings.setReminderTime(LocalTime.of(7, 5, 30))

        assertEquals(LocalTime.of(7, 5), LocalSettingsRepository(preferences, database, photoStore, dispatcher).reminderTime.value)
    }

    @Test
    fun restTimes_defaultAndPersist() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        assertEquals(RestTimes(betweenSetsSeconds = 90, betweenExercisesSeconds = 120), settings.restTimes.value)

        settings.setRestTimes(RestTimes(betweenSetsSeconds = 0, betweenExercisesSeconds = 180))

        assertEquals(
            RestTimes(betweenSetsSeconds = 0, betweenExercisesSeconds = 180),
            LocalSettingsRepository(preferences, database, photoStore, dispatcher).restTimes.value,
        )
    }

    @Test
    fun notificationPermissionRequested_persists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        assertEquals(false, LocalSettingsRepository(preferences, database, photoStore, dispatcher).notificationPermissionRequested.value)

        LocalSettingsRepository(preferences, database, photoStore, dispatcher).markNotificationPermissionRequested()

        assertTrue(LocalSettingsRepository(preferences, database, photoStore, dispatcher).notificationPermissionRequested.value)
    }

    @Test
    fun deleteAllData_removesStatsButKeepsPreferences() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val settings = LocalSettingsRepository(preferences, database, photoStore, dispatcher)
        val stats = OfflineFirstCustomStatRepository(database, time, ids)
        stats.createStat(CustomStat("", "Waist", "cm", 1, null, StatIcon.RULER, StatColor.TEAL))
        settings.setThemeMode(ThemeMode.DARK)

        settings.deleteAllData()

        assertTrue(stats.observeOverviews().first().isEmpty())
        assertEquals(ThemeMode.DARK, settings.themeMode.value)
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
        photos.savePhoto(null, PhotoPose.FRONT, capture)
        val saved = photos.observeCheckIns().first().single().photos.getValue(PhotoPose.FRONT)

        LocalSettingsRepository(preferences, database, photoStore, dispatcher).deleteAllData()

        assertTrue(photos.observeCheckIns().first().isEmpty())
        assertFalse(java.io.File(saved.filePath).exists())
    }
}
