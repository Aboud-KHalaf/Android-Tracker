package com.example.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tracker.domain.model.PhotoPose

/**
 * A progress photo; the picture itself is a file named [fileName] in the photos folder.
 * Unlike the other tables this one is local-only, so it has no sync columns and deleted rows
 * are removed along with their file.
 */
@Entity(
    tableName = "progress_photo",
    indices = [Index(value = ["epoch_day", "pose"], unique = true)],
)
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    /** [java.time.LocalDate.toEpochDay] of the day, in the user's calendar. */
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "pose") val pose: PhotoPose,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
