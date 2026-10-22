package com.example.tracker.domain.repository

/**
 * Backs up everything on this device (exercises, programs, workouts, stats and progress
 * photos) to one file, and restores it here or on another device.
 */
interface BackupRepository {
    /** Writes a backup to [destination], a content URI from the system file picker. Throws when it can't. */
    suspend fun backUp(destination: String)

    /**
     * Replaces all data on this device with the backup at [source], a content URI from the
     * system file picker. Preferences stay. When it fails nothing is changed, and it throws
     * [InvalidBackupException] or [NewerBackupException] when the file is the reason.
     */
    suspend fun restore(source: String)
}

/** The file isn't a backup made by this app, or it is damaged. */
class InvalidBackupException(cause: Throwable? = null) : Exception("Not a valid backup", cause)

/** The backup was made by a newer version of the app, which this one can't read. */
class NewerBackupException : Exception("Backup is from a newer app version")
