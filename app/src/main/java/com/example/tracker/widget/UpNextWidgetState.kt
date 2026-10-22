package com.example.tracker.widget

import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.program.nextWorkout
import com.example.tracker.domain.progress.summarizeWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** What the "Up next" widget shows; like Home's top card, plus this week's workout count. */
data class UpNextWidgetState(val today: LocalDate, val upNext: UpNext, val workoutsThisWeek: Int) {
    sealed interface UpNext {
        data class Resume(val name: String) : UpNext

        /** [scheduledOn] is null for a day without a fixed weekday; [daysAhead] says how far off it is. */
        data class Start(val name: String, val exerciseCount: Int, val scheduledOn: LocalDate?) : UpNext {
            fun daysAhead(today: LocalDate): Long? = scheduledOn?.let { ChronoUnit.DAYS.between(today, it) }
        }

        /** There is no program, or none of its days has exercises yet. */
        data object NothingToStart : UpNext
    }

    companion object {
        /** Works out the state as of [today] in [zone]. Pure. */
        fun of(
            today: LocalDate,
            zone: ZoneId,
            activeWorkout: Workout?,
            programs: List<Program>,
            history: List<WorkoutSummary>,
        ): UpNextWidgetState {
            val upNext = when {
                activeWorkout != null -> UpNext.Resume(activeWorkout.name)
                else -> {
                    val program = programs.firstOrNull { it.isActive } ?: programs.firstOrNull()
                    program?.let { nextWorkout(it, today, zone) }
                        ?.let { UpNext.Start(it.day.name, it.day.exerciseCount, it.date) }
                        ?: UpNext.NothingToStart
                }
            }
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return UpNextWidgetState(today, upNext, summarizeWeek(history, weekStart, zone).workoutCount)
        }
    }
}
