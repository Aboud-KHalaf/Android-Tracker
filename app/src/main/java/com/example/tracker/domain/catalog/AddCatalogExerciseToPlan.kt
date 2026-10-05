package com.example.tracker.domain.catalog

import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import kotlinx.coroutines.flow.first

/**
 * Adds a catalog exercise to a plan. A library exercise with the same name (ignoring case) is
 * reused, keeping its type, so its history stays in one place; otherwise one is created with
 * [type]. Failures propagate from the repositories.
 */
class AddCatalogExerciseToPlan(
    private val exercises: ExerciseRepository,
    private val plans: PlanRepository,
) {
    /** Returns the id of the library exercise that was added. */
    suspend operator fun invoke(planId: String, name: String, type: ExerciseType): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name must not be blank" }
        val existing = exercises.observeExercises().first()
            .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        val exerciseId = existing?.id ?: exercises.createExercise(trimmed, type)
        plans.addExercise(planId, exerciseId)
        return exerciseId
    }
}
