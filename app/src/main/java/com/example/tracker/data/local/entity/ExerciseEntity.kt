package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.MuscleGroup

@Entity(
    tableName = "exercises",
    indices = [Index("sync_state")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: ExerciseType,
    @Embedded val sync: SyncMetadata,
    @ColumnInfo(name = "muscle_group") val muscleGroup: MuscleGroup? = null,
)
