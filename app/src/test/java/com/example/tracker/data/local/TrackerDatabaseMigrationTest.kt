package com.example.tracker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun migrate2To3_keepsNutritionAndAddsWeightTable() {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO daily_nutrition (epoch_day, calories, protein_grams, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 2300, 150, 0, 0, NULL, 'PENDING')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 3, true).use { db ->
            db.query("SELECT calories FROM daily_nutrition").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2300, cursor.getInt(0))
            }
            db.execSQL(
                """
                INSERT INTO body_weight (epoch_day, weight_kg, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 79.4, 0, 0, NULL, 'PENDING')
                """
            )
        }
    }

    @Test
    fun migrate3To4_keepsNutritionAndAddsEmptySteps() {
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO daily_nutrition (epoch_day, calories, protein_grams, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 2300, 150, 0, 0, NULL, 'PENDING')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 4, true).use { db ->
            db.query("SELECT calories, steps FROM daily_nutrition").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2300, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate4To5_keepsWeightAndAddsProgressPhotoTable() {
        helper.createDatabase(DB_NAME, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO body_weight (epoch_day, weight_kg, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 79.4, 0, 0, NULL, 'PENDING')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 5, true).use { db ->
            db.query("SELECT weight_kg FROM body_weight").use { cursor ->
                cursor.moveToFirst()
                assertEquals(79.4, cursor.getDouble(0), 0.0)
            }
            db.execSQL(
                """
                INSERT INTO progress_photo (id, epoch_day, pose, file_name, created_at)
                VALUES ('p1', 20000, 'FRONT', 'p1.jpg', 0)
                """
            )
        }
    }

    @Test
    fun migrate5To6_movesExistingPlansIntoOneActiveProgram() {
        helper.createDatabase(DB_NAME, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO workout_plans (id, name, created_at, updated_at, deleted_at, sync_state)
                VALUES ('p1', 'Push Day', 0, 0, NULL, 'SYNCED'), ('p2', 'Old', 0, 0, 5, 'SYNCED')
                """
            )
            db.execSQL(
                """
                INSERT INTO workouts (id, plan_id, name, started_at, finished_at, created_at, updated_at, deleted_at, sync_state)
                VALUES ('w1', 'p1', 'Push Day', 0, 10, 0, 0, NULL, 'SYNCED')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 6, true).use { db ->
            db.query("SELECT id, name, is_active FROM programs").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(ProgramsMigration.LEGACY_PROGRAM_NAME, cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
            }
            db.query("SELECT id, program_id IS NOT NULL, day_of_week FROM workout_plans ORDER BY id").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(1)) // the live plan joined the program
                assertTrue(cursor.isNull(2)) // unscheduled until the user picks a weekday
                cursor.moveToNext()
                assertEquals(0, cursor.getInt(1)) // the deleted plan stays out of it
            }
            db.query("SELECT current_exercise_position FROM workouts").use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
            }
        }
    }

    @Test
    fun migrate5To6_withoutPlans_createsNoProgram() {
        helper.createDatabase(DB_NAME, 5).close()

        helper.runMigrationsAndValidate(DB_NAME, 6, true).use { db ->
            db.query("SELECT COUNT(*) FROM programs").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate6To7_keepsWeightAndAddsCustomStatTables() {
        helper.createDatabase(DB_NAME, 6).use { db ->
            db.execSQL(
                """
                INSERT INTO body_weight (epoch_day, weight_kg, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 79.4, 0, 0, NULL, 'PENDING')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 7, true).use { db ->
            db.query("SELECT weight_kg FROM body_weight").use { cursor ->
                cursor.moveToFirst()
                assertEquals(79.4, cursor.getDouble(0), 1e-9)
            }
            db.execSQL(
                """
                INSERT INTO custom_stats (id, name, unit, decimals, target, icon, color, created_at, updated_at, deleted_at, sync_state)
                VALUES ('s1', 'Body fat', '%', 1, 15.0, 'PERCENT', 'ORANGE', 0, 0, NULL, 'PENDING')
                """
            )
            db.execSQL(
                """
                INSERT INTO custom_stat_entries (stat_id, epoch_day, value, created_at, updated_at, deleted_at, sync_state)
                VALUES ('s1', 20000, 19.5, 0, 0, NULL, 'PENDING')
                """
            )
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
