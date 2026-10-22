package com.example.tracker.domain.model

/** How sets of an exercise are measured. */
enum class ExerciseType {
    /** Weight (kg) × reps, e.g. Bench Press. */
    WEIGHT_REPS,

    /** Hold time in seconds, e.g. Plank. */
    DURATION,
}

data class Exercise(
    val id: String,
    val name: String,
    val type: ExerciseType,
    /** The body part it mainly trains; null until the user (or the catalog) says. */
    val muscleGroup: MuscleGroup? = null,
)

/** Broad body parts, for weekly training volume. */
enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    ARMS,
    LEGS,
    CORE;

    companion object {
        /** The group for an exercise catalog category such as "Chest" or "Calves"; null for e.g. "Cardio". */
        fun fromCatalogCategory(category: String?): MuscleGroup? = when (category?.trim()?.lowercase()) {
            "chest" -> CHEST
            "back" -> BACK
            "shoulders" -> SHOULDERS
            "arms" -> ARMS
            "legs", "calves" -> LEGS
            "abs" -> CORE
            else -> null
        }
    }
}
