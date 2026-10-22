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

    @Test
    fun migrate7To8_turnsEachDayOfPhotosIntoOneSession() {
        helper.createDatabase(DB_NAME, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO progress_photo (id, epoch_day, pose, file_name, created_at)
                VALUES ('f1', 20000, 'FRONT', 'f1.jpg', 500), ('s1', 20000, 'SIDE', 's1.jpg', 400),
                       ('f2', 20007, 'FRONT', 'f2.jpg', 900)
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 8, true, PhotoSessionsMigration).use { db ->
            db.query("SELECT id, epoch_day, taken_at FROM photo_session ORDER BY epoch_day").use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals(20000, cursor.getLong(1))
                assertEquals(400, cursor.getLong(2)) // when the day's first photo was taken
            }
            db.query(
                """
                SELECT p.id, p.file_name, s.epoch_day FROM progress_photo AS p
                JOIN photo_session AS s ON s.id = p.session_id ORDER BY p.id
                """
            ).use { cursor ->
                assertEquals(3, cursor.count)
                cursor.moveToFirst()
                assertEquals("f1", cursor.getString(0))
                assertEquals("f1.jpg", cursor.getString(1))
                assertEquals(20000, cursor.getLong(2))
            }
            // A second session the same day can now hold its own front photo.
            db.execSQL("INSERT INTO photo_session (id, epoch_day, taken_at) VALUES ('later', 20000, 1000)")
            db.execSQL(
                "INSERT INTO progress_photo (id, session_id, pose, file_name, created_at) " +
                    "VALUES ('f3', 'later', 'FRONT', 'f3.jpg', 1000)",
            )
            db.execSQL("INSERT INTO photo_measurement (session_id, position, name, unit, value) VALUES ('later', 0, 'Waist', 'cm', 84.5)")
        }
    }

    @Test
    fun migrate7To8_withoutPhotos_createsNoSession() {
        helper.createDatabase(DB_NAME, 7).close()

        helper.runMigrationsAndValidate(DB_NAME, 8, true, PhotoSessionsMigration).use { db ->
            db.query("SELECT COUNT(*) FROM photo_session").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate8To9_turnsWeightAndTheDailyLogIntoStats() {
        helper.createDatabase(DB_NAME, 8).use { db ->
            db.execSQL(
                """
                INSERT INTO body_weight (epoch_day, weight_kg, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 80.5, 1, 1, NULL, 'SYNCED'), (20001, 80.1, 2, 2, NULL, 'SYNCED'), (20002, 79.0, 3, 3, 9, 'PENDING')
                """
            )
            db.execSQL(
                """
                INSERT INTO daily_nutrition (epoch_day, calories, protein_grams, steps, created_at, updated_at, deleted_at, sync_state)
                VALUES (20000, 2300, 150, NULL, 1, 1, NULL, 'SYNCED')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 9, true, LegacyLogsMigration).use { db ->
            db.query("SELECT id, name, unit, decimals FROM custom_stats ORDER BY created_at").use { cursor ->
                val stats = buildList {
                    while (cursor.moveToNext()) {
                        add("${cursor.getString(0)} ${cursor.getString(1)} ${cursor.getString(2)} ${cursor.getInt(3)}")
                    }
                }
                // No steps were logged, so there is no Steps stat.
                assertEquals(
                    listOf("legacy-weight Weight kg 1", "legacy-calories Calories kcal 0", "legacy-protein Protein g 0"),
                    stats,
                )
            }
            db.query("SELECT epoch_day, value FROM custom_stat_entries WHERE stat_id = 'legacy-weight' ORDER BY epoch_day").use { cursor ->
                assertEquals(2, cursor.count) // the deleted entry stays behind
                cursor.moveToFirst()
                assertEquals(80.5, cursor.getDouble(1), 1e-9)
            }
            db.query("SELECT value FROM custom_stat_entries WHERE stat_id = 'legacy-protein'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(150.0, cursor.getDouble(0), 1e-9)
            }
        }
    }

    @Test
    fun migrate8To9_withoutLogs_createsNoStats() {
        helper.createDatabase(DB_NAME, 8).close()

        helper.runMigrationsAndValidate(DB_NAME, 9, true, LegacyLogsMigration).use { db ->
            db.query("SELECT COUNT(*) FROM custom_stats").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }

    @Test
    fun migrate9To10_existingSetsAreNormalSets() {
        helper.createDatabase(DB_NAME, 9).use { db ->
            db.execSQL(
                """
                INSERT INTO workout_sets (id, workout_exercise_id, position, weight_kg, reps, duration_seconds, completed_at,
                    created_at, updated_at, deleted_at, sync_state)
                VALUES ('s1', 'we1', 0, 60.0, 8, NULL, 1, 0, 0, NULL, 'SYNCED')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 10, true).use { db ->
            db.query("SELECT set_type, weight_kg FROM workout_sets WHERE id = 's1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("NORMAL", cursor.getString(0))
                assertEquals(60.0, cursor.getDouble(1), 0.0)
            }
        }
    }

    @Test
    fun migrate10To11_existingSetsAreUnrated() {
        helper.createDatabase(DB_NAME, 10).use { db ->
            db.execSQL(
                """
                INSERT INTO workout_sets (id, workout_exercise_id, position, weight_kg, reps, duration_seconds, completed_at,
                    set_type, created_at, updated_at, deleted_at, sync_state)
                VALUES ('s1', 'we1', 0, 60.0, 8, NULL, 1, 'WARM_UP', 0, 0, NULL, 'SYNCED')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 11, true).use { db ->
            db.query("SELECT rpe, set_type FROM workout_sets WHERE id = 's1'").use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
                assertEquals("WARM_UP", cursor.getString(1))
            }
        }
    }

    @Test
    fun migrate11To12_addsEmptyNotes() {
        helper.createDatabase(DB_NAME, 11).use { db ->
            db.execSQL(
                """
                INSERT INTO workouts (id, plan_id, name, started_at, finished_at, current_exercise_position,
                    created_at, updated_at, deleted_at, sync_state)
                VALUES ('w1', NULL, 'Push', 0, 1, NULL, 0, 0, NULL, 'SYNCED')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 12, true).use { db ->
            db.query("SELECT name, note FROM workouts WHERE id = 'w1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Push", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate12To13_existingExercisesAreInNoSuperset() {
        helper.createDatabase(DB_NAME, 12).use { db ->
            db.execSQL(
                """
                INSERT INTO plan_exercises (id, plan_id, exercise_id, position, target_sets,
                    created_at, updated_at, deleted_at, sync_state)
                VALUES ('pe1', 'p1', 'e1', 0, 3, 0, 0, NULL, 'SYNCED')
                """
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 13, true).use { db ->
            db.query("SELECT superset_group, target_sets FROM plan_exercises WHERE id = 'pe1'").use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
                assertEquals(3, cursor.getInt(1))
            }
        }
    }
}
