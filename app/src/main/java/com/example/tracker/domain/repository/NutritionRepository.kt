package com.example.tracker.domain.repository

import com.example.tracker.domain.model.DailyNutrition
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Calories and protein per day, at most one entry per day. Write functions throw
 * [IllegalArgumentException] for out-of-range values; storage failures propagate as exceptions.
 */
interface NutritionRepository {
    /** Logged days from [from] to [to], both inclusive, newest first. */
    fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DailyNutrition>>

    fun observeDay(date: LocalDate): Flow<DailyNutrition?>

    /** Logs [day], replacing anything already logged for that date. */
    suspend fun saveDay(day: DailyNutrition)

    suspend fun deleteDay(date: LocalDate)
}
