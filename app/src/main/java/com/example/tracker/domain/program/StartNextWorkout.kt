package com.example.tracker.domain.program

import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.repository.ProgramRepository
import com.example.tracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

/**
 * "Start my workout" in one step, as Home's main button does: the workout already in progress
 * if there is one, otherwise a new one from the active program's next training day (see
 * [nextWorkout]). Returns the workout's id, or null when there is no day to start.
 */
class StartNextWorkout(
    private val programs: ProgramRepository,
    private val workouts: WorkoutRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(): String? {
        workouts.observeActiveWorkout().first()?.let { return it.id }
        val all = programs.observePrograms().first()
        val program = all.firstOrNull { it.isActive } ?: all.firstOrNull() ?: return null
        val today = time.now().atZone(time.zone()).toLocalDate()
        val next = nextWorkout(program, today, time.zone()) ?: return null
        return workouts.startWorkout(next.day.id)
    }
}
