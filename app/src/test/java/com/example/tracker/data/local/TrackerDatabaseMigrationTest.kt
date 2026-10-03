package com.example.tracker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrackerDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TrackerDatabase::class.java)

    @Test
    fun migrate1To2_keepsExistingDataAndAddsNutritionTable() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO exercises (id, name, type, created_at, updated_at, deleted_at, sync_state)
                VALUES ('e1', 'Bench Press', 'WEIGHT_REPS', 0, 0, NULL, 'PENDING')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 2, true).use { db ->
            db.query("SELECT name FROM exercises").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Bench Press", cursor.getString(0))
            }
            db.execSQL(
                """
                INSERT INTO daily_nutrition (epoch_day, calories, protein_grams, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 2300, 150, 0, 0, NULL, 'PENDING')
                """
            )
            db.query("SELECT COUNT(*) FROM daily_nutrition").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
