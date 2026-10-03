package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata

/**
 * A performed workout. [finishedAt] is null while the workout is in progress.
 * [name] is a snapshot of the plan name, so history survives plan renames and deletes.
 */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            deferred = true,
        ),
    ],
    indices = [Index("plan_id"), Index("finished_at"), Index("sync_state")],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_id") val planId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    @Embedded val sync: SyncMetadata,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            deferred = true,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            deferred = true,
        ),
    ],
    indices = [Index("workout_id"), Index("exercise_id"), Index("sync_state")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "position") val position: Int,
    @Embedded val sync: SyncMetadata,
)

/**
 * One set. Weight-and-reps exercises use [weightKg] and [reps]; duration exercises use
 * [durationSeconds]. [completedAt] is null until the set is completed.
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            deferred = true,
        ),
    ],
    indices = [Index("workout_exercise_id"), Index("sync_state")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @Embedded val sync: SyncMetadata,
)
