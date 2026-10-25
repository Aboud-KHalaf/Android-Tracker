package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.data.local.SyncMetadata

/** A number the user tracks. [icon], [color] and [goal] are enum names, read leniently. */
@Entity(
    tableName = "custom_stats",
    indices = [Index("sync_state")],
)
data class CustomStatEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "decimals") val decimals: Int,
    @ColumnInfo(name = "target") val target: Double?,
    @ColumnInfo(name = "icon") val icon: String,
    @ColumnInfo(name = "color") val color: String,
    @Embedded val sync: SyncMetadata,
    @ColumnInfo(name = "goal", defaultValue = "CHANGE") val goal: String = "CHANGE",
    /** Where the user placed it on the Stats tab; stats with the same position go oldest first. */
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0,
)

/** A stat's value on one day; the stat and day are the key, so there is one per day. */
@Entity(
    tableName = "custom_stat_entries",
    primaryKeys = ["stat_id", "epoch_day"],
    foreignKeys = [
        ForeignKey(
            entity = CustomStatEntity::class,
            parentColumns = ["id"],
            childColumns = ["stat_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sync_state")],
)
data class CustomStatEntryEntity(
    @ColumnInfo(name = "stat_id") val statId: String,
    /** [java.time.LocalDate.toEpochDay] of the day, in the user's calendar. */
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "value") val value: Double,
    @Embedded val sync: SyncMetadata,
)
