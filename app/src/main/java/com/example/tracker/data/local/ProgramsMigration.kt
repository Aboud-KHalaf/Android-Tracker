package com.example.tracker.data.local

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 6 turns plans into the training days of programs. Room adds the new table and
 * columns; this moves plans that already exist into one active program, so nothing the user
 * built is lost. Their days stay unscheduled until the user picks weekdays for them.
 */
class ProgramsMigration : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO programs (id, name, is_active, created_at, updated_at, deleted_at, sync_state)
            SELECT lower(hex(randomblob(16))), ?, 1, ?, ?, NULL, 'PENDING'
            WHERE EXISTS (SELECT 1 FROM workout_plans WHERE deleted_at IS NULL)
            """,
            arrayOf<Any>(LEGACY_PROGRAM_NAME, now, now),
        )
        db.execSQL(
            """
            UPDATE workout_plans
            SET program_id = (SELECT id FROM programs LIMIT 1), updated_at = ?, sync_state = 'PENDING'
            WHERE program_id IS NULL AND deleted_at IS NULL
            """,
            arrayOf<Any>(now),
        )
    }

    companion object {
        /** Stored data rather than UI text, so the user can rename it like any program. */
        const val LEGACY_PROGRAM_NAME = "My program"
    }
}
