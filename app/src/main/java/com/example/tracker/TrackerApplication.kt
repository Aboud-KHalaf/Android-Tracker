package com.example.tracker

import android.app.Application
import com.example.tracker.di.AppContainer
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Keep the reminder alarm in line with the setting: set it now, and again on every change.
        container.nutritionReminder.sync()
        container.settingsRepository.nutritionReminderEnabled
            .drop(1)
            .onEach { container.nutritionReminder.sync() }
            .launchIn(container.applicationScope)
    }
}
