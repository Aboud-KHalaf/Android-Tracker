package com.example.tracker.ui.settings

import com.example.tracker.R
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.text
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseSession
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.ui.exercise.ExerciseReportBuilder
import com.example.tracker.ui.stats.detail.StatReportBuilder
import java.time.LocalDate
import java.time.ZoneId

/** Builds the "Export all data" report from every section's full history. Pure. */
internal class FullExportReportBuilder(zone: ZoneId) {
    private val exerciseReports = ExerciseReportBuilder(zone)

    fun build(
        exportedOn: LocalDate,
        exercises: List<Exercise>,
        sessions: List<ExerciseSession>,
        /** Each stat with all its entries. */
        stats: List<Pair<CustomStat, List<StatEntry>>>,
    ): Report = Report(
        title = text(R.string.export_all_title),
        subtitle = text(R.string.export_exported_on, exportedOn),
        sections = buildList {
            add(exerciseReports.exercisesSection(exercises, sessions))
            add(exerciseReports.setsSection(sessions, exerciseNames = exercises.associate { it.id to it.name }))
            stats.forEach { (stat, entries) -> addAll(StatReportBuilder.sections(stat, entries)) }
        },
    )
}
