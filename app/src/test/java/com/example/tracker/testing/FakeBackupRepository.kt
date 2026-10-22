package com.example.tracker.testing

import com.example.tracker.domain.repository.BackupRepository
import kotlinx.coroutines.CompletableDeferred

class FakeBackupRepository : BackupRepository {
    val backups = mutableListOf<String>()
    val restores = mutableListOf<String>()

    /** When set, every backup and restore fails with it. */
    var error: Exception? = null

    /** When set, backups wait for it to complete. */
    var backUpGate: CompletableDeferred<Unit>? = null

    override suspend fun backUp(destination: String) {
        backUpGate?.await()
        error?.let { throw it }
        backups += destination
    }

    override suspend fun restore(source: String) {
        error?.let { throw it }
        restores += source
    }
}
