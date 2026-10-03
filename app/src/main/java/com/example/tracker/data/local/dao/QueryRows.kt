package com.example.tracker.data.local.dao

import androidx.room.ColumnInfo
import com.example.tracker.domain.model.ExerciseType

/** A plan with its active exercise count and the time it was last finished. */
data class PlanSummaryRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
    @ColumnInfo(name = "last_done_at") val lastDoneAt: Long?,
)

/** A plan exercise joined with its exercise. */
data class PlanExerciseRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "exercise_type") val exerciseType: ExerciseType,
)

/** A workout exercise joined with its exercise. */
data class WorkoutExerciseRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "exercise_type") val exerciseType: ExerciseType,
)

/** A finished workout with its exercise count, for history lists. */
data class WorkoutSummaryRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "plan_id") val planId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
)

/** A completed set in a finished workout, with the context needed for progress stats. */
data class LoggedSetRow(
    @ColumnInfo(name = "workout_id") val workoutId: String,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "exercise_type") val exerciseType: ExerciseType,
    @ColumnInfo(name = "set_id") val setId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
)
