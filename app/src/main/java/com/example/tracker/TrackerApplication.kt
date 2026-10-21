package com.example.tracker

import android.app.Application
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.tracker.di.AppContainer
import com.example.tracker.widget.UpNextWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch(Dispatchers.IO) {
            try {
                container.legacyNutritionTargets.moveIntoStats()
            } catch (e: Exception) {
                // Retried on the next start; the stats work without their targets meanwhile.
                Log.w(TAG, "Couldn't move the daily log targets onto stats", e)
            }
        }
        // Keep the reminder alarm in line with the settings: set it now, and again on every change.
        container.dailyReminder.sync()
        val settings = container.settingsRepository
        combine(settings.reminderEnabled, settings.reminderTime, ::Pair)
            .drop(1)
            .onEach { container.dailyReminder.sync() }
            .launchIn(container.applicationScope)
        // Mirror the workout in progress, if any, in its notification; this also restores it
        // when the app starts again after the process was ended.
        container.activeWorkoutSession.keepInSync(container.applicationScope)
        // Keep the home screen widget's "Up next" in line with workouts and programs.
        val workouts = container.workoutRepository
        combine(
            workouts.observeActiveWorkout().map { it?.id },
            workouts.observeHistory().map { it.size },
            container.programRepository.observePrograms(),
        ) { _, _, _ -> }
            .drop(1)
            .onEach { UpNextWidget().updateAll(this) }
            .catch { Log.w(TAG, "Couldn't keep the widget up to date", it) }
            .launchIn(container.applicationScope)
    }

    private companion object {
        const val TAG = "TrackerApplication"
    }
}
