package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.domain.model.PhotoPose

/*
 * Progress photos are local-only, so these tables have no sync columns and deleted rows are
 * removed (with their files) rather than kept as tombstones.
 */

/** A photo session: the photos taken together and the measurements noted with them. */
@Entity(tableName = "photo_session")
data class PhotoSessionEntity(
    @PrimaryKey val id: String,
    /** [java.time.LocalDate.toEpochDay] of the day, in the user's calendar. */
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    /** When the session's first photo was kept, in epoch milliseconds. */
    @ColumnInfo(name = "taken_at") val takenAt: Long,
)

/** A progress photo; the picture itself is a file named [fileName] in the photos folder. */
@Entity(
    tableName = "progress_photo",
    foreignKeys = [
        ForeignKey(
            entity = PhotoSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id", "pose"], unique = true)],
)
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "pose") val pose: PhotoPose,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** A measurement noted with a photo session, kept in the order it was entered. */
@Entity(
    tableName = "photo_measurement",
    primaryKeys = ["session_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PhotoMeasurementEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "value") val value: Double,
)
