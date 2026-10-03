package com.example.tracker.di

import android.content.Context
import com.example.tracker.core.IdGenerator
import com.example.tracker.core.SystemTimeProvider
import com.example.tracker.core.TimeProvider
import com.example.tracker.core.UuidGenerator
import com.example.tracker.data.local.TrackerDatabase
import com.example.tracker.data.repository.OfflineFirstExerciseRepository
import com.example.tracker.data.repository.OfflineFirstPlanRepository
import com.example.tracker.data.repository.OfflineFirstWorkoutRepository
import com.example.tracker.domain.repository.ExerciseRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.WorkoutRepository

/**
 * App-wide dependencies, created once in [com.example.tracker.TrackerApplication].
 * ViewModels receive repositories from here through their factories.
 */
class AppContainer(context: Context) {
    val time: TimeProvider = SystemTimeProvider
    private val ids: IdGenerator = UuidGenerator
    private val database: TrackerDatabase by lazy { TrackerDatabase.create(context.applicationContext) }

    val exerciseRepository: ExerciseRepository by lazy { OfflineFirstExerciseRepository(database, time, ids) }
    val planRepository: PlanRepository by lazy { OfflineFirstPlanRepository(database, time, ids) }
    val workoutRepository: WorkoutRepository by lazy { OfflineFirstWorkoutRepository(database, time, ids) }
}
