package com.example.tracker.data.repository

import com.example.tracker.data.local.dao.LoggedSetRow
import com.example.tracker.data.local.dao.PlanExerciseRow
import com.example.tracker.data.local.dao.PlanSummaryRow
import com.example.tracker.data.local.dao.WorkoutExerciseRow
import com.example.tracker.data.local.dao.WorkoutSummaryRow
import com.example.tracker.data.local.entity.ExerciseEntity
import com.example.tracker.data.local.entity.WorkoutEntity
import com.example.tracker.data.local.entity.WorkoutSetEntity
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.LoggedSet
import com.example.tracker.domain.model.PlanExercise
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutExercise
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSet
import com.example.tracker.domain.model.WorkoutSummary
import java.time.DayOfWeek
import java.time.Instant

internal fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

/** Weekdays are stored as ISO numbers, 1 = Monday. */
internal fun Int.toDayOfWeek(): DayOfWeek = DayOfWeek.of(this)

internal fun ExerciseEntity.toDomain() = Exercise(id = id, name = name, type = type)

internal fun PlanSummaryRow.toDomain() = WorkoutPlan(
    id = id,
    name = name,
    exerciseCount = exerciseCount,
    lastDoneAt = lastDoneAt?.toInstant(),
    programId = programId,
    dayOfWeek = dayOfWeek?.toDayOfWeek(),
)

internal fun PlanExerciseRow.toDomain() = PlanExercise(
    id = id,
    exercise = Exercise(exerciseId, exerciseName, exerciseType),
    position = position,
    config = ExerciseConfig(
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        targetDurationSeconds = targetDurationSeconds,
    ),
)

internal fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    position = position,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    completedAt = completedAt?.toInstant(),
    type = type,
    rpe = rpe,
)

internal fun WorkoutEntity.toDomain(
    exercises: List<WorkoutExerciseRow>,
    sets: List<WorkoutSetEntity>,
): Workout {
    val setsByExercise = sets.groupBy { it.workoutExerciseId }
    return Workout(
        id = id,
        planId = planId,
        name = name,
        startedAt = startedAt.toInstant(),
        finishedAt = finishedAt?.toInstant(),
        currentExerciseIndex = currentExercisePosition,
        note = note,
        exercises = exercises.map { row ->
            WorkoutExercise(
                id = row.id,
                exercise = Exercise(row.exerciseId, row.exerciseName, row.exerciseType),
                position = row.position,
                sets = setsByExercise[row.id].orEmpty().map { it.toDomain() },
                note = row.note,
            )
        },
    )
}

internal fun WorkoutSummaryRow.toDomain(personalBestCount: Int) = WorkoutSummary(
    id = id,
    planId = planId,
    name = name,
    startedAt = startedAt.toInstant(),
    finishedAt = finishedAt.toInstant(),
    exerciseCount = exerciseCount,
    personalBestCount = personalBestCount,
    note = note,
)

internal fun LoggedSetRow.toDomain() = LoggedSet(
    workoutId = workoutId,
    finishedAt = finishedAt.toInstant(),
    exerciseId = exerciseId,
    exerciseType = exerciseType,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    exerciseNote = exerciseNote,
)
