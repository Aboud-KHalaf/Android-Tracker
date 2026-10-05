package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata

/**
 * What was eaten, and how many steps were walked, on one day. The day itself is the key, so there is at most one row per
 * day on every device and rows from different devices merge naturally when synced.
 */
@Entity(
    tableName = "daily_nutrition",
    indices = [Index("sync_state")],
)
data class DailyNutritionEntity(
    /** [java.time.LocalDate.toEpochDay] of the day, in the user's calendar. */
    @PrimaryKey @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "calories") val calories: Int,
    @ColumnInfo(name = "protein_grams") val proteinGrams: Int,
    /** Null when the day's steps weren't logged. */
    @ColumnInfo(name = "steps") val steps: Int?,
    @Embedded val sync: SyncMetadata,
)
