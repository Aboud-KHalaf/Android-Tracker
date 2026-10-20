package com.example.tracker.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tracker.data.local.dao.CustomStatDao
import com.example.tracker.data.local.dao.ExerciseDao
import com.example.tracker.data.local.dao.PlanDao
import com.example.tracker.data.local.dao.ProgramDao
import com.example.tracker.data.local.dao.ProgressPhotoDao
import com.example.tracker.data.local.dao.WorkoutDao
import com.example.tracker.data.local.entity.CustomStatEntity
import com.example.tracker.data.local.entity.CustomStatEntryEntity
import com.example.tracker.data.local.entity.ExerciseEntity
import com.example.tracker.data.local.entity.PhotoMeasurementEntity
import com.example.tracker.data.local.entity.PhotoSessionEntity
import com.example.tracker.data.local.entity.PlanEntity
import com.example.tracker.data.local.entity.PlanExerciseEntity
import com.example.tracker.data.local.entity.ProgramEntity
import com.example.tracker.data.local.entity.ProgressPhotoEntity
import com.example.tracker.data.local.entity.WorkoutEntity
import com.example.tracker.data.local.entity.WorkoutExerciseEntity
import com.example.tracker.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        ProgramEntity::class,
        PlanEntity::class,
        PlanExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        ProgressPhotoEntity::class,
        CustomStatEntity::class,
        CustomStatEntryEntity::class,
        PhotoSessionEntity::class,
        PhotoMeasurementEntity::class,
    ],
    version = 9,
    exportSchema = true,
    autoMigrations = [
        // Adds daily_nutrition.
        AutoMigration(from = 1, to = 2),
        // Adds body_weight.
        AutoMigration(from = 2, to = 3),
        // Adds daily_nutrition.steps.
        AutoMigration(from = 3, to = 4),
        // Adds progress_photo.
        AutoMigration(from = 4, to = 5),
        // Adds programs, weekday training days, exercise targets and the current exercise.
        AutoMigration(from = 5, to = 6, spec = ProgramsMigration::class),
        // Adds custom_stats and custom_stat_entries.
        AutoMigration(from = 6, to = 7),
        // 7 to 8 groups progress photos into sessions, and 8 to 9 turns the weight and daily
        // logs into custom stats: PhotoSessionsMigration and LegacyLogsMigration, added below.
    ],
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDao(): ProgramDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun customStatDao(): CustomStatDao

    companion object {
        private const val NAME = "tracker.db"

        fun create(context: Context): TrackerDatabase =
            Room.databaseBuilder(context, TrackerDatabase::class.java, NAME)
                .addMigrations(PhotoSessionsMigration, LegacyLogsMigration)
                .build()
    }
}
