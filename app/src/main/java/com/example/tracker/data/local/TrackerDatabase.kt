package com.example.tracker.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tracker.data.local.dao.ExerciseDao
import com.example.tracker.data.local.dao.NutritionDao
import com.example.tracker.data.local.dao.PlanDao
import com.example.tracker.data.local.dao.ProgramDao
import com.example.tracker.data.local.dao.ProgressPhotoDao
import com.example.tracker.data.local.dao.WeightDao
import com.example.tracker.data.local.dao.WorkoutDao
import com.example.tracker.data.local.entity.BodyWeightEntity
import com.example.tracker.data.local.entity.DailyNutritionEntity
import com.example.tracker.data.local.entity.ExerciseEntity
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
        DailyNutritionEntity::class,
        BodyWeightEntity::class,
        ProgressPhotoEntity::class,
    ],
    version = 6,
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
    ],
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDao(): ProgramDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun weightDao(): WeightDao
    abstract fun progressPhotoDao(): ProgressPhotoDao

    companion object {
        private const val NAME = "tracker.db"

        fun create(context: Context): TrackerDatabase =
            Room.databaseBuilder(context, TrackerDatabase::class.java, NAME).build()
    }
}
