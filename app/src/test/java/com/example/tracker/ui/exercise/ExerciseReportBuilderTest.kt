package com.example.tracker.ui.exercise

import com.example.tracker.R
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.plainCell
import com.example.tracker.domain.export.textCell
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.domain.progress.benchHistory
import com.example.tracker.domain.progress.holdSet
import com.example.tracker.domain.progress.toSessions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseReportBuilderTest {

    private val builder = ExerciseReportBuilder(ZoneOffset.UTC)
    private val now = Instant.parse("2026-10-03T09:00:00Z")
    private val bench = Exercise("bench", "Bench Press", ExerciseType.WEIGHT_REPS)
    private val plank = Exercise("plank", "Plank", ExerciseType.DURATION)

    private fun figures(metric: ProgressMetric?) =
        builder.build(bench, benchHistory.toSessions(), metric, TimeRange.ALL, now)
            .sections.first().summary.associate { (it.label as ReportText.Res).id to it.value }

    @Test
    fun build_defaultMetric_summarizesTopSetProgressAndBest() {
        val figures = figures(metric = null)

        assertEquals(textCell(R.string.metric_weight), figures[R.string.export_metric])
        assertEquals(numberCell(6), figures[R.string.export_sessions_in_range])
        assertEquals(numberCell(40.0, 1), figures[R.string.export_first_value])
        assertEquals(numberCell(10.0, 1), figures[R.string.export_change_value])
        assertEquals(textCell(R.string.set_weight_reps, 50.0, 8), figures[R.string.exercise_personal_best])
        assertEquals(ReportCell.Date(LocalDate.of(2026, 9, 5)), figures[R.string.export_personal_best_date])
    }

    @Test
    fun build_listsSessionsInRangeWithStatsAndPersonalBests() {
        val report = builder.build(bench, benchHistory.toSessions(), ProgressMetric.WEIGHT, TimeRange.ONE_MONTH, now)

        val sessions = report.sections[1].table!!
        assertEquals(
            listOf(
                listOf(
                    ReportCell.Date(LocalDate.of(2026, 9, 5)),
                    numberCell(3),
                    numberCell(50.0, 1),
                    numberCell(8),
                    numberCell(26),
                    numberCell(1250.0, 1),
                    textCell(R.string.export_yes),
                ),
            ),
            sessions.rows,
        )
        val sets = report.sections[2].table!!
        assertEquals(3, sets.rows.size)
        assertEquals(
            listOf(ReportCell.Date(LocalDate.of(2026, 9, 5)), numberCell(1), numberCell(45.0, 1), numberCell(10)),
            sets.rows.first(),
        )
    }

    @Test
    fun build_holdExercise_usesHoldColumns() {
        val sessions = listOf(holdSet("w1", 0, 45), holdSet("w2", 7, 60)).toSessions()

        val report = builder.build(plank, sessions, metric = null, TimeRange.ALL, now)

        assertEquals(4, report.sections[1].table!!.headers.size)
        assertEquals(
            listOf(ReportCell.Date(LocalDate.of(2026, 8, 8)), numberCell(1), numberCell(60)),
            report.sections[2].table!!.rows.last(),
        )
    }

    @Test
    fun setsSection_withNames_listsMixedExercisesWithAllColumns() {
        val sessions = (benchHistory.take(1) + holdSet("w1", 0, 45)).toSessions()

        val table = builder.setsSection(sessions, mapOf("bench" to "Bench Press", "plank" to "Plank")).table!!

        assertEquals(6, table.headers.size)
        assertEquals(plainCell("Plank"), table.rows.first { it[5] != ReportCell.Empty }[1])
    }

    @Test
    fun exercisesSection_summarizesEachExerciseByName() {
        val table = builder.exercisesSection(listOf(plank, bench), benchHistory.toSessions()).table!!

        assertEquals(listOf(plainCell("Bench Press"), plainCell("Plank")), table.rows.map { it.first() })
        assertEquals(numberCell(6), table.rows.first()[1])
        assertEquals(numberCell(50.0, 1), table.rows.first()[4])
        assertEquals(numberCell(0), table.rows.last()[1])
    }
}
