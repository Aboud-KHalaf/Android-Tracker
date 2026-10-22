package com.example.tracker.domain.workout

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutSet
import java.time.Duration

/** What a finished workout added up to, for its summary. */
data class WorkoutRecap(
    val duration: Duration,
    val completedSetCount: Int,
    /** Weight × reps over every completed set, kg. */
    val volumeKg: Double,
    /** The same volume for the previous finished workout of the same training day, if any. */
    val previousVolumeKg: Double?,
    /** Exercises with at least one completed set, in workout order. */
    val exercises: List<ExerciseRecap>,
    /** Personal bests set in this workout, in workout order. */
    val personalBests: List<PersonalBest>,
) {
    /** Volume compared with [previousVolumeKg]; null without a previous workout or volume. */
    val volumeChangeKg: Double? get() = previousVolumeKg?.takeIf { it > 0.0 && volumeKg > 0.0 }?.let { volumeKg - it }
}

data class ExerciseRecap(val exercise: Exercise, val sets: List<WorkoutSet>)

/**
 * Sums up a finished [workout]. [personalBests] may hold every personal best; only this
 * workout's are kept. [previous] is the workout before it from the same training day.
 */
fun recapOf(workout: Workout, personalBests: List<PersonalBest>, previous: Workout?): WorkoutRecap? {
    val finishedAt = workout.finishedAt ?: return null
    val exercises = workout.exercises.sortedBy { it.position }.mapNotNull { exercise ->
        val sets = exercise.sets.filter { it.isCompleted }.sortedBy { it.position }
        if (sets.isEmpty()) null else ExerciseRecap(exercise.exercise, sets)
    }
    val order = exercises.map { it.exercise.id }
    return WorkoutRecap(
        duration = Duration.between(workout.startedAt, finishedAt),
        completedSetCount = exercises.sumOf { it.sets.size },
        volumeKg = workout.volumeKg(),
        previousVolumeKg = previous?.volumeKg(),
        exercises = exercises,
        personalBests = personalBests.filter { it.workoutId == workout.id }.sortedBy { order.indexOf(it.exerciseId) },
    )
}

private fun Workout.volumeKg(): Double =
    exercises.flatMap { it.sets }.filter { it.isCompleted }.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
