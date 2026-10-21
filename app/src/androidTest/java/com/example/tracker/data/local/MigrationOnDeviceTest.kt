package com.example.tracker.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every database version migrates to the latest on the device's own SQLite, which can differ
 * from Robolectric's (the unit tests check each step's data). The latest version is read from
 * the exported schemas, so a new migration is covered without editing this test.
 */
@RunWith(AndroidJUnit4::class)
class MigrationOnDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, TrackerDatabase::class.java)

    private val latestVersion: Int =
        instrumentation.context.assets.list(TrackerDatabase::class.java.name).orEmpty()
            .mapNotNull { it.removeSuffix(".json").toIntOrNull() }
            .max()

    @Test
    fun everyVersion_migratesToTheLatest() {
        for (version in 1 until latestVersion) {
            val name = "migration-$version.db"
            helper.createDatabase(name, version).close()
            helper.runMigrationsAndValidate(name, latestVersion, true, *MANUAL_MIGRATIONS).close()
        }
    }

    @Test
    fun firstVersion_keepsItsExercises_andOpensWithRoom() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO exercises (id, name, type, created_at, updated_at, deleted_at, sync_state)
                VALUES ('e1', 'Bench Press', 'WEIGHT_REPS', 0, 0, NULL, 'PENDING')
                """
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, latestVersion, true, *MANUAL_MIGRATIONS).close()

        // The app's own builder opens the migrated file, so the schema matches what Room expects.
        val database = Room.databaseBuilder(instrumentation.targetContext, TrackerDatabase::class.java, DB_NAME)
            .addMigrations(*MANUAL_MIGRATIONS)
            .build()
        try {
            database.openHelper.readableDatabase.query("SELECT name FROM exercises").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Bench Press", cursor.getString(0))
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-on-device.db"

        /** The migrations TrackerDatabase.create adds by hand; the others are automatic. */
        val MANUAL_MIGRATIONS = arrayOf(PhotoSessionsMigration, LegacyLogsMigration)
    }
}
