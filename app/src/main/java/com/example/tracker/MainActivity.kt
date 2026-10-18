package com.example.tracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tracker.ui.TrackerApp
import com.example.tracker.ui.TrackerAppViewModel
import com.example.tracker.ui.splash.installAnimatedSplashScreen
import com.example.tracker.ui.theme.TrackerTheme
import com.example.tracker.workout.AndroidActiveWorkoutNotifier

class MainActivity : ComponentActivity() {
    private val appViewModel: TrackerAppViewModel by viewModels { TrackerAppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installAnimatedSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // With saved state, Android restores the screens the user was on, workout included.
        if (savedInstanceState == null) {
            appViewModel.onFreshStart(
                requestedWorkoutId = intent.workoutToOpen(),
                restoreActiveWorkout = intent.action == Intent.ACTION_MAIN,
            )
        }
        val settings = (application as TrackerApplication).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDark(systemIsDark = isSystemInDarkTheme())

            // Match the status and navigation bar icons to the app's theme, not the device's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }

            TrackerTheme(darkTheme = darkTheme) {
                val workoutToOpen by appViewModel.workoutToOpen.collectAsStateWithLifecycle()
                TrackerApp(workoutToOpen = workoutToOpen, onWorkoutOpened = appViewModel::onWorkoutOpened)
            }
        }
    }

    /** The workout notification was tapped while the app was already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.workoutToOpen()?.let(appViewModel::onOpenWorkout)
    }

    /**
     * The workout a notification tap asks for. Reopening from Recents can replay an old tap
     * for a workout that has ended since, so that is ignored.
     */
    private fun Intent.workoutToOpen(): String? =
        takeIf { it.action == AndroidActiveWorkoutNotifier.ACTION_OPEN_WORKOUT }
            ?.takeIf { it.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0 }
            ?.getStringExtra(AndroidActiveWorkoutNotifier.EXTRA_WORKOUT_ID)
}

// The default scrims `enableEdgeToEdge()` uses for three-button navigation.
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
