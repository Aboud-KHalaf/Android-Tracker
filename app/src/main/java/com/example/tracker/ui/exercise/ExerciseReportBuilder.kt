package com.example.tracker.ui.exercise

import androidx.annotation.StringRes
import com.example.tracker.R
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportEntry
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportTable
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.plainCell
import com.example.tracker.domain.export.text
import com.example.tracker.domain.export.textCell
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PersonalBest
import com.example.tracker.domain.progress.BodyweightLog
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.domain.progress.currentPersonalBest
import com.example.tracker.domain.progress.findPersonalBests
import com.example.tracker.domain.progress.stats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Builds exercise reports: one exercise's progress over a range, and the workout tables the
 * full export shares. Pure.
 */
internal class ExerciseReportBuilder(private val zone: ZoneId) {

    /** [metric] null means the exercise type's default, as on screen. */
    fun build(
        exercise: Exercise,
        sessions: List<ExerciseSession>,
        metric: ProgressMetric?,
        range: TimeRange,
        now: Instant,
        bodyweight: BodyweightLog? = null,
    ): Report {
        val metrics = ProgressMetric.forType(exercise.type, withBodyweight = bodyweight?.isEmpty == false)
        val chosen = metric?.takeIf { it in metrics } ?: metrics.first()
        val ordered = sessions.sortedBy { it.finishedAt }
        val inRange = ordered.filter { range.contains(it.finishedAt, now, zone) }
        val best = currentPersonalBest(ordered)
        val values = inRange.mapNotNull { chosen.valueOf(it, bodyweight?.on(it.finishedAt.toDate())) }
        val unit = text(chosen.unitRes)
        val decimals = chosen.decimals

        return Report(
            title = ReportText.Plain(exercise.name),
            subtitle = text(R.string.export_range_exported_on, text(range.descriptionRes), now.toDate()),
            sections = listOf(
                ReportSection(
                    title = text(R.string.export_section_summary),
                    summary = buildList {
                        add(ReportEntry(text(R.string.export_metric), textCell(chosen.labelRes)))
                        add(ReportEntry(text(R.string.export_range), textCell(range.descriptionRes)))
                        add(ReportEntry(text(R.string.export_sessions_in_range), numberCell(inRange.size)))
                        add(ReportEntry(text(R.string.export_sessions_total), numberCell(ordered.size)))
                        if (values.isNotEmpty()) {
                            add(ReportEntry(text(R.string.export_first_value, unit), numberCell(values.first(), decimals)))
                            add(ReportEntry(text(R.string.export_latest_value, unit), numberCell(values.last(), decimals)))
                            add(ReportEntry(text(R.string.export_change_value, unit), numberCell(values.last() - values.first(), decimals)))
                        }
                        if (best != null) {
                            add(ReportEntry(text(R.string.exercise_personal_best), best.valueCell()))
                            add(ReportEntry(text(R.string.export_personal_best_date), ReportCell.Date(best.achievedAt.toDate())))
                        }
                    },
                ),
                sessionsSection(exercise.type, inRange, personalBestIds = findPersonalBests(ordered).map { it.workoutId }.toSet()),
                setsSection(inRange, exerciseNames = null),
            ),
        )
    }

    /** One row per exercise: how often and how well it was trained. */
    fun exercisesSection(exercises: List<Exercise>, sessions: List<ExerciseSession>): ReportSection {
        val byExercise = sessions.groupBy { it.exerciseId }
        return ReportSection(
            title = text(R.string.export_section_exercises),
            table = ReportTable(
                headers = listOf(
                    text(R.string.export_column_exercise),
                    text(R.string.export_sessions_total),
                    text(R.string.export_column_first_session),
                    text(R.string.export_column_last_session),
                    text(R.string.export_column_top_weight_kg),
                    text(R.string.export_column_top_set_reps),
                    text(R.string.export_column_longest_hold_seconds),
                ),
                rows = exercises.sortedBy { it.name.lowercase() }.map { exercise ->
                    val ordered = byExercise[exercise.id].orEmpty().sortedBy { it.finishedAt }
                    val best = currentPersonalBest(ordered)
                    listOf(
                        plainCell(exercise.name),
                        numberCell(ordered.size),
                        ordered.firstOrNull()?.let { ReportCell.Date(it.finishedAt.toDate()) } ?: ReportCell.Empty,
                        ordered.lastOrNull()?.let { ReportCell.Date(it.finishedAt.toDate()) } ?: ReportCell.Empty,
                        numberCell(best?.weightKg, WEIGHT_DECIMALS),
                        numberCell(best?.reps),
                        numberCell(best?.durationSeconds),
                    )
                },
            ),
        )
    }

    /**
     * Every set, oldest first. With [exerciseNames], sessions of several exercises are listed
     * together with an Exercise column.
     */
    fun setsSection(sessions: List<ExerciseSession>, exerciseNames: Map<String, String>?): ReportSection {
        val types = sessions.map { it.exerciseType }.toSet()
        val hasWeights = ExerciseType.WEIGHT_REPS in types
        val hasHolds = ExerciseType.DURATION in types
        val headers = buildList {
            add(text(R.string.export_column_date))
            if (exerciseNames != null) add(text(R.string.export_column_exercise))
            add(text(R.string.export_column_set))
            if (hasWeights) {
                add(text(R.string.export_column_weight_kg))
                add(text(R.string.export_column_reps))
            }
            if (hasHolds) add(text(R.string.export_column_hold_seconds))
        }
        val rows = sessions.sortedBy { it.finishedAt }.flatMap { session ->
            session.sets.mapIndexed { index, set ->
                buildList {
                    add(ReportCell.Date(session.finishedAt.toDate()))
                    if (exerciseNames != null) add(plainCell(exerciseNames[session.exerciseId].orEmpty()))
                    add(numberCell(index + 1))
                    if (hasWeights) {
                        add(numberCell(set.weightKg, WEIGHT_DECIMALS))
                        add(numberCell(set.reps))
                    }
                    if (hasHolds) add(numberCell(set.durationSeconds))
                }
            }
        }
        return ReportSection(title = text(R.string.export_section_sets), table = ReportTable(headers, rows))
    }

    private fun sessionsSection(type: ExerciseType, sessions: List<ExerciseSession>, personalBestIds: Set<String>): ReportSection {
        val headers = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(
                R.string.export_column_date,
                R.string.export_column_sets,
                R.string.export_column_top_weight_kg,
                R.string.export_column_top_set_reps,
                R.string.export_column_total_reps,
                R.string.export_column_volume_kg,
                R.string.export_column_personal_best,
            )
            ExerciseType.DURATION -> listOf(
                R.string.export_column_date,
                R.string.export_column_sets,
                R.string.export_column_longest_hold_seconds,
                R.string.export_column_personal_best,
            )
        }
        val rows = sessions.map { session ->
            val stats = session.stats()
            val date = ReportCell.Date(session.finishedAt.toDate())
            val isBest = if (session.workoutId in personalBestIds) textCell(R.string.export_yes) else ReportCell.Empty
            when (type) {
                ExerciseType.WEIGHT_REPS -> listOf(
                    date,
                    numberCell(session.sets.size),
                    numberCell(stats.topWeightKg, WEIGHT_DECIMALS),
                    numberCell(stats.topSetReps),
                    numberCell(stats.totalReps),
                    numberCell(stats.volumeKg, WEIGHT_DECIMALS),
                    isBest,
                )
                ExerciseType.DURATION -> listOf(date, numberCell(session.sets.size), numberCell(stats.longestHoldSeconds), isBest)
            }
        }
        return ReportSection(
            title = text(R.string.export_section_sessions),
            table = ReportTable(headers.map { text(it) }, rows),
        )
    }

    private fun PersonalBest.valueCell(): ReportCell = when {
        weightKg != null && reps != null -> textCell(R.string.set_weight_reps, weightKg, reps)
        durationSeconds != null -> textCell(R.string.export_hold_value, durationSeconds)
        else -> ReportCell.Empty
    }

    private fun Instant.toDate(): LocalDate = atZone(zone).toLocalDate()

    private val ProgressMetric.decimals: Int
        get() = when (this) {
            ProgressMetric.WEIGHT, ProgressMetric.VOLUME -> WEIGHT_DECIMALS
            ProgressMetric.RELATIVE_STRENGTH -> RATIO_DECIMALS
            ProgressMetric.REPS, ProgressMetric.HOLD -> 0
        }

    @get:StringRes
    private val ProgressMetric.unitRes: Int
        get() = when (this) {
            ProgressMetric.WEIGHT, ProgressMetric.VOLUME -> R.string.export_unit_kg
            ProgressMetric.REPS -> R.string.export_unit_reps
            ProgressMetric.HOLD -> R.string.export_unit_seconds
            ProgressMetric.RELATIVE_STRENGTH -> R.string.export_unit_bodyweight
        }

    private companion object {
        const val WEIGHT_DECIMALS = 1
        const val RATIO_DECIMALS = 2
    }
}
