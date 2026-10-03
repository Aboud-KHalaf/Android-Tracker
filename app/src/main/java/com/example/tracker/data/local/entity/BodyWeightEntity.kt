package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata

/** Body weight on one day; like nutrition, the day is the key, so there is one entry per day. */
@Entity(
    tableName = "body_weight",
    indices = [Index("sync_state")],
)
data class BodyWeightEntity(
    /** [java.time.LocalDate.toEpochDay] of the day, in the user's calendar. */
    @PrimaryKey @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @Embedded val sync: SyncMetadata,
)
