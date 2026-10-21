package com.example.tracker.domain.model

import java.time.Duration
import java.time.Instant

/**
 * A performed workout with its exercises and sets. [finishedAt] is null while in progress.
 * [currentExerciseIndex] is the exercise the user last had open, if they picked one.
 */
data class Workout(
    val id: String,
    val planId: String?,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exercises: List<WorkoutExercise>,
    val currentExerciseIndex: Int? = null,
    /** How the workout went, in the user's words. */
    val note: String? = null,
) {
    val isInProgress: Boolean get() = finishedAt == null
}

data class WorkoutExercise(
    val id: String,
    val exercise: Exercise,
    val position: Int,
    val sets: List<WorkoutSet>,
    /** Something to remember about this exercise in this workout, e.g. "left shoulder hurt". */
    val note: String? = null,
    /** Shared by the exercises of a superset; see [com.example.tracker.domain.workout.Supersets]. */
    val supersetGroup: String? = null,
)

/** Rules for workout and exercise notes. */
object WorkoutNote {
    const val MAX_LENGTH = 500

    /** The note to store for [text]: trimmed, or null when blank. */
    fun normalize(text: String?): String? {
        val note = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        require(note.length <= MAX_LENGTH) { "A note is at most $MAX_LENGTH characters" }
        return note
    }
}

/**
 * One set. Weight-and-reps exercises use [weightKg] and [reps]; duration exercises use
 * [durationSeconds]. [completedAt] is null until the set is completed.
 */
data class WorkoutSet(
    val id: String,
    val position: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val completedAt: Instant?,
    val type: SetType = SetType.NORMAL,
    /** How hard the set felt, on the RPE scale ([Rpe.MIN] to [Rpe.MAX]); null if not rated. */
    val rpe: Double? = null,
) {
    val isCompleted: Boolean get() = completedAt != null
}

/** Rate of perceived exertion: 10 is a maximal set, 9 leaves one rep in reserve, 8 two, and so on. */
object Rpe {
    const val MIN = 1.0
    const val MAX = 10.0

    /** The ratings offered while logging; below 6 a set is too easy to rate usefully. */
    val choices: List<Double> = listOf(6.0, 7.0, 8.0, 9.0, 10.0)
}

/** What kind of set it is. Only warm-ups are left out of progress, records and comparisons. */
enum class SetType {
    NORMAL,
    WARM_UP,
    DROP,
    FAILURE;

    val isWarmUp: Boolean get() = this == WARM_UP
}

/** A finished workout as shown in history lists. */
data class WorkoutSummary(
    val id: String,
    val planId: String?,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val exerciseCount: Int,
    val personalBestCount: Int,
    val note: String? = null,
) {
    val duration: Duration get() = Duration.between(startedAt, finishedAt)
}
