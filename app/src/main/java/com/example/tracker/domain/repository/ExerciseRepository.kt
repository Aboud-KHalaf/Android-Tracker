package com.example.tracker.domain.repository

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import kotlinx.coroutines.flow.Flow

/**
 * Exercise library. Write functions throw [IllegalArgumentException] for invalid input or
 * unknown ids; storage failures propagate as exceptions.
 */
interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>
    fun observeExercise(id: String): Flow<Exercise?>

    /** Returns the new exercise's id. */
    suspend fun createExercise(name: String, type: ExerciseType): String
    suspend fun renameExercise(id: String, name: String)

    /** Removes the exercise from the library and from all plans. Workout history keeps it. */
    suspend fun deleteExercise(id: String)
}
