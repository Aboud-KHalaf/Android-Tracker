package com.example.tracker.ui.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.domain.repository.SettingsRepository
import com.example.tracker.ui.common.launchCatching
import com.example.tracker.ui.program.ProgramSetup
import com.example.tracker.ui.program.schedule
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/** The first-launch steps, in order. */
enum class OnboardingStep { WELCOME, PROGRAM, STATS }

/** What onboarding shows: the current step, and whether something is being saved. */
data class OnboardingUiState(val step: OnboardingStep = OnboardingStep.WELCOME, val isSaving: Boolean = false)

sealed interface OnboardingEvent {
    /** Onboarding is over; [programId] is the program made in it, to open next, if any. */
    data class Finished(val programId: String?) : OnboardingEvent

    data class ShowMessage(@StringRes val messageRes: Int) : OnboardingEvent
}

/**
 * First launch: a welcome, an optional first program and the stats to track. Every step can
 * be skipped; finishing or skipping marks onboarding as done, so it isn't shown again. The
 * step and the program made survive Android ending the app, through [savedState].
 */
class OnboardingViewModel(
    private val programs: ProgramRepository,
    private val stats: CustomStatRepository,
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val locale: () -> Locale = Locale::getDefault,
) : ViewModel() {

    private val step = savedState.getStateFlow(KEY_STEP, OnboardingStep.WELCOME)
    private val isSaving = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> = combine(step, isSaving, ::OnboardingUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingUiState(step.value))

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    private var programId: String?
        get() = savedState[KEY_PROGRAM_ID]
        set(value) {
            savedState[KEY_PROGRAM_ID] = value
        }

    fun onGetStarted() = goTo(OnboardingStep.PROGRAM)

    fun onSkipProgram() = goTo(OnboardingStep.STATS)

    /** Back from a step goes to the one before it. */
    fun onBack() {
        when (step.value) {
            OnboardingStep.WELCOME -> Unit
            OnboardingStep.PROGRAM -> goTo(OnboardingStep.WELCOME)
            OnboardingStep.STATS -> goTo(OnboardingStep.PROGRAM)
        }
    }

    fun onCreateProgram(setup: ProgramSetup) {
        val name = setup.name.trim()
        if (name.isEmpty() || isSaving.value) return
        save(onError = R.string.home_error_create_program) {
            programId = programs.createProgram(name, setup.schedule(locale()))
            goTo(OnboardingStep.STATS)
        }
    }

    /** Makes the chosen [newStats] and ends onboarding. */
    fun onFinish(newStats: List<CustomStat>) {
        if (isSaving.value) return
        save(onError = R.string.onboarding_error_stats) {
            newStats.forEach { stats.createStat(it) }
            finish()
        }
    }

    /** Leaves onboarding from the welcome, setting nothing up. */
    fun onSkipAll() {
        save(onError = R.string.onboarding_error_stats) { finish() }
    }

    private suspend fun finish() {
        settings.setOnboardingDone()
        _events.send(OnboardingEvent.Finished(programId))
    }

    private fun goTo(next: OnboardingStep) {
        savedState[KEY_STEP] = next
    }

    private fun save(@StringRes onError: Int, block: suspend () -> Unit) {
        isSaving.value = true
        viewModelScope.launchCatching(onError = { _events.send(OnboardingEvent.ShowMessage(onError)) }) {
            try {
                block()
            } finally {
                isSaving.value = false
            }
        }
    }

    companion object {
        private const val KEY_STEP = "onboarding_step"
        private const val KEY_PROGRAM_ID = "onboarding_program_id"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                OnboardingViewModel(
                    programs = container.programRepository,
                    stats = container.customStatRepository,
                    settings = container.settingsRepository,
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}
