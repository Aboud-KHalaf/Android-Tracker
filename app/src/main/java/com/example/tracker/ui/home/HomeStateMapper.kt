package com.example.tracker.ui.home

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.model.WeekSummary
import com.example.tracker.domain.model.Workout
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.domain.plan.suggestNextPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Maps domain data to [HomeUiState.Success]. Pure, so it can be tested without a ViewModel. */
internal class HomeStateMapper(private val zone: ZoneId) {

    fun map(
        today: LocalDate,
        activeWorkout: Workout?,
        plans: List<WorkoutPlan>,
        week: WeekSummary,
        personalBests: List<PersonalBest>,
        exercises: List<Exercise>,
        history: List<WorkoutSummary>,
    ): HomeUiState.Success = HomeUiState.Success(
        today = today,
        upNext = upNext(activeWorkout, plans),
        week = WeekUi(
            days = weekDays(today, week),
            workoutCount = week.workoutCount,
            totalDuration = week.totalDuration,
            personalBest = latestPersonalBestThisWeek(today, personalBests, exercises),
        ),
        plans = plans.map { PlanItemUi(it.id, it.name, it.exerciseCount) },
        recentWorkouts = history.take(RECENT_WORKOUT_LIMIT).map { it.toRecentWorkoutUi() },
    )

    private fun upNext(activeWorkout: Workout?, plans: List<WorkoutPlan>): UpNextUi {
        if (activeWorkout != null) {
            return UpNextUi.Resume(
                workoutId = activeWorkout.id,
                name = activeWorkout.name,
                startedAt = activeWorkout.startedAt.atZone(zone).toLocalTime(),
            )
        }
        val plan = suggestNextPlan(plans) ?: return UpNextUi.NoPlans
        return UpNextUi.Start(
            planId = plan.id,
            name = plan.name,
            exerciseCount = plan.exerciseCount,
            lastDoneOn = plan.lastDoneAt?.atZone(zone)?.toLocalDate(),
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

    private fun WorkoutSummary.toRecentWorkoutUi() = RecentWorkoutUi(
        id = id,
        name = name,
        date = startedAt.atZone(zone).toLocalDate(),
        exerciseCount = exerciseCount,
        duration = duration,
        personalBestCount = personalBestCount,
    )

    companion object {
        const val RECENT_WORKOUT_LIMIT = 3

        fun startOfWeek(date: LocalDate): LocalDate =
            date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    }
}
