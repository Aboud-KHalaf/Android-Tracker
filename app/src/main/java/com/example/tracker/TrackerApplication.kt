package com.example.tracker

import android.app.Application
import com.example.tracker.di.AppContainer

class TrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
