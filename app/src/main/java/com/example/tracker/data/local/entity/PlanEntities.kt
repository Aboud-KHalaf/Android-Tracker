package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata

/** A workout program, e.g. "Push/Pull/Legs". At most one is active at a time. */
@Entity(
    tableName = "programs",
    indices = [Index("sync_state")],
)
data class ProgramEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_active", defaultValue = "0") val isActive: Boolean,
    @Embedded val sync: SyncMetadata,
)

/**
 * A training day of a program with its own exercise list. [dayOfWeek] is the ISO weekday
 * (1 = Monday) it is scheduled on, or null when it isn't tied to a weekday. [position] is
 * where the user placed it in the program, or null until they arrange the days.
 */
@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_id"],
            deferred = true,
        ),
    ],
    indices = [Index("program_id"), Index("sync_state")],
)
data class PlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "program_id") val programId: String?,
    @ColumnInfo(name = "day_of_week") val dayOfWeek: Int?,
    @Embedded val sync: SyncMetadata,
    @ColumnInfo(name = "position") val position: Int? = null,
)

/**
 * An exercise in a plan, ordered by [position], with its default configuration. The targets
 * are what the user plans to do; what they actually did is recorded in workout sets.
 */
@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            deferred = true,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            deferred = true,
        ),
    ],
    indices = [Index("plan_id"), Index("exercise_id"), Index("sync_state")],
)
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "target_reps") val targetReps: Int? = null,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double? = null,
    @ColumnInfo(name = "target_duration_seconds") val targetDurationSeconds: Int? = null,
    @ColumnInfo(name = "superset_group") val supersetGroup: String? = null,
    @Embedded val sync: SyncMetadata,
)
