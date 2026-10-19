package com.example.tracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 8 groups progress photos into sessions, so taking photos again the same day keeps
 * the earlier ones, and adds measurements noted with a session. Each day that already has
 * photos becomes one session, taken when its first photo was, so no photo is lost.
 *
 * Written by hand because the photos' new session column must be filled from the day
 * before its foreign key can hold.
 */
object PhotoSessionsMigration : Migration(7, 8) {
    // The connection overload is the one Room calls with every SQLite driver.
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `photo_session` (`id` TEXT NOT NULL, `epoch_day` INTEGER NOT NULL, " +
                "`taken_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            """
            INSERT INTO photo_session (id, epoch_day, taken_at)
            SELECT '$LEGACY_SESSION_PREFIX' || epoch_day, epoch_day, MIN(created_at) FROM progress_photo GROUP BY epoch_day
            """,
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `progress_photo_new` (`id` TEXT NOT NULL, `session_id` TEXT NOT NULL, " +
                "`pose` TEXT NOT NULL, `file_name` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`session_id`) REFERENCES `photo_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        connection.execSQL(
            """
            INSERT INTO progress_photo_new (id, session_id, pose, file_name, created_at)
            SELECT id, '$LEGACY_SESSION_PREFIX' || epoch_day, pose, file_name, created_at FROM progress_photo
            """,
        )
        connection.execSQL("DROP TABLE progress_photo")
        connection.execSQL("ALTER TABLE progress_photo_new RENAME TO progress_photo")
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_progress_photo_session_id_pose` ON `progress_photo` (`session_id`, `pose`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `photo_measurement` (`session_id` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, `unit` TEXT NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`session_id`, `position`), " +
                "FOREIGN KEY(`session_id`) REFERENCES `photo_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }

    /** Sessions made from a day's photos are named this plus the epoch day. */
    const val LEGACY_SESSION_PREFIX = "day-"
}
