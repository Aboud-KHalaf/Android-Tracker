package com.example.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.onboarding.FirstLaunch
import com.example.tracker.domain.repository.WorkoutRepository
import com.example.tracker.ui.common.launchCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * App-level navigation that doesn't start from a screen: reopening the workout in progress when
 * the app starts afresh (e.g. after Android ended it), or when its notification is tapped.
 */
class TrackerAppViewModel(
    private val workoutRepository: WorkoutRepository,
    private val needsOnboarding: suspend () -> Boolean = { false },
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
        // Someone opening the app for the first time is welcomed; it has no workout to resume.
        viewModelScope.launchCatching(onError = {}) {
            if (needsOnboarding()) _showOnboarding.value = true
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

    private val _showOnboarding = MutableStateFlow(false)

    /** True when the app should open onboarding, until [onOnboardingShown]. */
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    fun onOnboardingShown() {
        _showOnboarding.value = false
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                val firstLaunch = FirstLaunch(
                    container.settingsRepository,
                    container.programRepository,
                    container.exerciseRepository,
                    container.customStatRepository,
                )
                TrackerAppViewModel(container.workoutRepository, needsOnboarding = firstLaunch::needsOnboarding)
            }
        }
    }
}
