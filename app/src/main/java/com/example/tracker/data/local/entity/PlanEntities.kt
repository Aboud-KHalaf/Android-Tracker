package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata

@Entity(
    tableName = "workout_plans",
    indices = [Index("sync_state")],
)
data class PlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @Embedded val sync: SyncMetadata,
)

/** An exercise in a plan, ordered by [position]. */
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
    @Embedded val sync: SyncMetadata,
)
