package com.example.tracker.data.local

import androidx.room.ColumnInfo

/** Whether a row's latest local change has been pushed to the backend. */
enum class SyncState {
    SYNCED,
    PENDING,
}

/**
 * Bookkeeping columns shared by every syncable table (embedded in each entity).
 *
 * Rows are never hard-deleted locally: [deletedAt] marks a tombstone so the deletion
 * can be pushed to the backend later. Every local change sets [syncState] to
 * [SyncState.PENDING]; a future sync engine pushes pending rows and marks them synced.
 */
data class SyncMetadata(
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING,
) {
    fun touched(now: Long): SyncMetadata = copy(updatedAt = now, syncState = SyncState.PENDING)

    fun deleted(now: Long): SyncMetadata =
        copy(updatedAt = now, deletedAt = now, syncState = SyncState.PENDING)

    fun restored(now: Long): SyncMetadata =
        copy(updatedAt = now, deletedAt = null, syncState = SyncState.PENDING)

    companion object {
        fun created(now: Long): SyncMetadata = SyncMetadata(createdAt = now, updatedAt = now)
    }
}
