package com.example.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.program.StartNextWorkout
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.launchCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * App-level navigation that doesn't start from a screen: reopening the workout in progress when
 * the app starts afresh (e.g. after Android ended it), when its notification is tapped, or when
 * a launcher shortcut asks for a workout or a screen.
 */
class TrackerAppViewModel(
    private val workoutRepository: WorkoutRepository,
    private val startNextWorkout: suspend () -> String?,
    private val syncHealthIfDue: suspend () -> Unit = {},
) : ViewModel() {

    private val _workoutToOpen = MutableStateFlow<String?>(null)

    /** A workout the app should navigate to, until [onWorkoutOpened]. */
    val workoutToOpen: StateFlow<String?> = _workoutToOpen.asStateFlow()

    /**
     * Called when the activity starts without saved state. Opens [requestedWorkoutId] if given;
     * otherwise, when [restoreActiveWorkout], the workout still in progress, if there is one.
     */
    fun onFreshStart(requestedWorkoutId: String?, restoreActiveWorkout: Boolean) {
        if (requestedWorkoutId != null) {
            _workoutToOpen.value = requestedWorkoutId
            return
        }
        if (!restoreActiveWorkout) return
        // If this fails, Home still offers to resume the workout.
        viewModelScope.launchCatching(onError = {}) {
            workoutRepository.observeActiveWorkout().first()?.let { _workoutToOpen.value = it.id }
        }
    }

    /** The workout's notification was tapped while the app was running. */
    fun onOpenWorkout(workoutId: String) {
        _workoutToOpen.value = workoutId
    }

    fun onWorkoutOpened() {
        _workoutToOpen.value = null
    }

    private val _screenToOpen = MutableStateFlow<AppScreen?>(null)

    /** A screen the app should navigate to, until [onScreenOpened]. */
    val screenToOpen: StateFlow<AppScreen?> = _screenToOpen.asStateFlow()

    /**
     * A launcher shortcut was used; [action] is one of [AppShortcuts]. "Start workout" resumes
     * the workout in progress or starts the next one, like Home's button; without a training
     * day to start, the app simply opens on Home, which says what's missing.
     */
    fun onShortcut(action: String) {
        when (action) {
            AppShortcuts.ACTION_START_WORKOUT -> viewModelScope.launchCatching(onError = {}) {
                startNextWorkout()?.let { _workoutToOpen.value = it }
            }
            AppShortcuts.ACTION_LOG_STAT -> _screenToOpen.value = AppScreen.STATS
            AppShortcuts.ACTION_TAKE_PHOTOS -> _screenToOpen.value = AppScreen.PHOTO_CAPTURE
        }
    }

    fun onScreenOpened() {
        _screenToOpen.value = null
    }

    /** The app came to the foreground: refresh steps, weight and sleep if that's turned on. */
    fun onAppStarted() {
        // Failing is fine: Settings shows when it last worked and offers to sync again.
        viewModelScope.launchCatching(onError = {}) { syncHealthIfDue() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                val startNextWorkout = StartNextWorkout(container.programRepository, container.workoutRepository, container.time)
                TrackerAppViewModel(
                    container.workoutRepository,
                    startNextWorkout = { startNextWorkout() },
                    syncHealthIfDue = { container.healthSync.syncIfDue() },
                )
            }
        }
    }
}
