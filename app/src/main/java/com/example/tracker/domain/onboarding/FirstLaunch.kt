package com.example.tracker.domain.onboarding

import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Decides whether to show onboarding: only to someone who hasn't seen it and has nothing in
 * the app yet. Anyone who already has programs, exercises or stats (e.g. updating from a
 * version without onboarding) is marked as done instead.
 */
class FirstLaunch(
    private val settings: SettingsRepository,
    private val programs: ProgramRepository,
    private val exercises: ExerciseRepository,
    private val stats: CustomStatRepository,
) {
    suspend fun needsOnboarding(): Boolean {
        if (settings.onboardingDone.value) return false
        val hasData = programs.observePrograms().first().isNotEmpty() ||
            exercises.observeExercises().first().isNotEmpty() ||
            stats.observeOverviews().first().isNotEmpty()
        if (hasData) settings.setOnboardingDone()
        return !hasData
    }
}
