package com.example.tracker.ui.home

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.program.nextWorkout
import com.example.tracker.ui.common.SetValueUi
import com.example.tracker.ui.common.toWorkoutSummaryUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Everything Home shows, as the repositories report it. */
internal data class HomeSources(
    val activeWorkout: Workout?,
    val programs: List<Program>,
    val week: WeekSummary,
    val personalBests: List<PersonalBest>,
    val exercises: List<Exercise>,
    val history: List<WorkoutSummary>,
)

/** Maps domain data to [HomeUiState.Success]. Pure, so it can be tested without a ViewModel. */
internal class HomeStateMapper(private val zone: ZoneId) {

    fun map(today: LocalDate, sources: HomeSources): HomeUiState.Success = with(sources) {
        HomeUiState.Success(
            today = today,
            upNext = upNext(today, activeWorkout, programs),
            week = WeekUi(
                days = weekDays(today, week),
                workoutCount = week.workoutCount,
                totalDuration = week.totalDuration,
                personalBest = latestPersonalBestThisWeek(today, personalBests, exercises),
            ),
            programs = programs.map { program ->
                ProgramItemUi(
                    id = program.id,
                    name = program.name,
                    isActive = program.isActive,
                    trainingDays = program.trainingDays.sorted(),
                    dayCount = program.days.size,
                )
            },
            recentWorkouts = history.take(RECENT_WORKOUT_LIMIT).map { it.toWorkoutSummaryUi(zone) },
        )
    }

    private fun upNext(today: LocalDate, activeWorkout: Workout?, programs: List<Program>): UpNextUi {
        if (activeWorkout != null) {
            return UpNextUi.Resume(
                workoutId = activeWorkout.id,
                name = activeWorkout.name,
                startedAt = activeWorkout.startedAt.atZone(zone).toLocalTime(),
            )
        }
        val program = programs.firstOrNull { it.isActive } ?: programs.firstOrNull() ?: return UpNextUi.NoPrograms
        val next = nextWorkout(program, today, zone) ?: return UpNextUi.EmptyProgram(program.id, program.name)
        return UpNextUi.Start(
            planId = next.day.id,
            name = next.day.name,
            exerciseCount = next.day.exerciseCount,
            lastDoneOn = next.day.lastDoneAt?.atZone(zone)?.toLocalDate(),
            scheduledOn = next.date,
        )
    }

    private fun weekDays(today: LocalDate, week: WeekSummary): List<WeekDayUi> =
        DayOfWeek.entries.map { day ->
            val status = when {
                day in week.trainedDays -> DayStatus.TRAINED
                day == today.dayOfWeek -> DayStatus.TODAY
                else -> DayStatus.IDLE
            }
            WeekDayUi(day, status)
        }

    private fun latestPersonalBestThisWeek(
        today: LocalDate,
        personalBests: List<PersonalBest>,
        exercises: List<Exercise>,
    ): PersonalBestUi? {
        val weekStart = startOfWeek(today)
        val best = personalBests
            .filter { !it.achievedAt.atZone(zone).toLocalDate().isBefore(weekStart) }
            .maxByOrNull { it.achievedAt }
            ?: return null
        // A deleted exercise has no name to show, so its record is skipped.
        val exercise = exercises.firstOrNull { it.id == best.exerciseId } ?: return null
        val value = best.toSetValue() ?: return null
        return PersonalBestUi(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            value = value,
            achievedOn = best.achievedAt.atZone(zone).dayOfWeek,
        )
    }

    private fun PersonalBest.toSetValue(): SetValueUi? = when {
        weightKg != null && reps != null -> SetValueUi.WeightReps(weightKg, reps)
        durationSeconds != null -> SetValueUi.Hold(durationSeconds)
        else -> null
    }

    companion object {
        const val RECENT_WORKOUT_LIMIT = 3

        fun startOfWeek(date: LocalDate): LocalDate =
            date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    }
}
