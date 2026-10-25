package com.example.tracker.domain.repository

import com.example.tracker.domain.model.Program
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

/**
 * Workout programs and their weekly schedule. A day's exercises are edited through
 * [PlanRepository]. While any program exists exactly one is active. Write functions throw
 * [IllegalArgumentException] for invalid input or unknown ids.
 */
interface ProgramRepository {
    fun observePrograms(): Flow<List<Program>>
    fun observeProgram(id: String): Flow<Program?>

    /**
     * Creates a program with one empty training day per entry of [schedule], named by its
     * value, and returns its id. The first program becomes the active one.
     */
    suspend fun createProgram(name: String, schedule: Map<DayOfWeek, String>): String
    suspend fun renameProgram(id: String, name: String)

    /** Deletes the program with its days; another program becomes active if it was. */
    suspend fun deleteProgram(id: String)
    suspend fun setActiveProgram(id: String)

    /**
     * Adds an empty training day and returns its id. It is scheduled on [dayOfWeek], which must
     * be free, or, when that is null, done whenever the user likes.
     */
    suspend fun addDay(programId: String, dayOfWeek: DayOfWeek?, name: String): String

    /** Moves a day to [dayOfWeek], which must be free in its program, or unschedules it (null). */
    suspend fun scheduleDay(planId: String, dayOfWeek: DayOfWeek?)

    /** Puts the days of [programId] in the order of [dayIds], which must list every day once. */
    suspend fun reorderDays(programId: String, dayIds: List<String>)
}
