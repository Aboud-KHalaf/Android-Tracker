package com.example.tracker.ui.nutrition

import com.example.tracker.R
import com.example.tracker.domain.export.Report
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportEntry
import com.example.tracker.domain.export.ReportSection
import com.example.tracker.domain.export.ReportTable
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.text
import com.example.tracker.domain.export.textCell
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.nutrition.NutritionSummary
import java.time.LocalDate

/** Builds the daily log report: the period's averages and target hits, then every logged day. Pure. */
internal object NutritionReportBuilder {

    fun build(
        from: LocalDate,
        to: LocalDate,
        exportedOn: LocalDate,
        days: List<DailyNutrition>,
        targets: NutritionTargets,
    ): Report = Report(
        title = text(R.string.nutrition_title),
        subtitle = text(R.string.export_period_exported_on, from, to, exportedOn),
        sections = sections(days, targets),
    )

    /** The summary and days sections, shared with the full export. */
    fun sections(days: List<DailyNutrition>, targets: NutritionTargets): List<ReportSection> {
        val summary = NutritionSummary.of(days, targets)
        val headers = buildList {
            add(text(R.string.export_column_date))
            add(text(R.string.export_column_calories))
            add(text(R.string.export_column_protein))
            add(text(R.string.export_column_steps))
            if (targets.calories != null) add(text(R.string.export_column_calories_on_target))
            if (targets.proteinGrams != null) add(text(R.string.export_column_protein_on_target))
            if (targets.steps != null) add(text(R.string.export_column_steps_on_target))
        }
        return listOf(
            ReportSection(
                title = text(R.string.nutrition_title),
                summary = buildList {
                    add(ReportEntry(text(R.string.export_logged_days), numberCell(days.size)))
                    if (summary != null) {
                        add(ReportEntry(text(R.string.export_average_calories), numberCell(summary.averageCalories)))
                        add(ReportEntry(text(R.string.export_average_protein), numberCell(summary.averageProteinGrams)))
                        summary.averageSteps?.let { add(ReportEntry(text(R.string.export_average_steps), numberCell(it))) }
                    }
                    targets.calories?.let { add(ReportEntry(text(R.string.export_calorie_target), numberCell(it))) }
                    targets.proteinGrams?.let { add(ReportEntry(text(R.string.export_protein_target), numberCell(it))) }
                    targets.steps?.let { add(ReportEntry(text(R.string.export_steps_target), numberCell(it))) }
                    summary?.caloriesOnTargetDays?.let {
                        add(ReportEntry(text(R.string.export_calories_on_target_days), numberCell(it)))
                    }
                    summary?.proteinOnTargetDays?.let {
                        add(ReportEntry(text(R.string.export_protein_on_target_days), numberCell(it)))
                    }
                    summary?.stepsOnTargetDays?.let {
                        add(ReportEntry(text(R.string.export_steps_on_target_days), numberCell(it)))
                    }
                },
            ),
            ReportSection(
                title = text(R.string.export_section_days),
                table = ReportTable(
                    headers = headers,
                    rows = days.sortedBy { it.date }.map { day ->
                        buildList {
                            add(ReportCell.Date(day.date))
                            add(numberCell(day.calories))
                            add(numberCell(day.proteinGrams))
                            add(numberCell(day.steps))
                            targets.calories?.let { add(yesIf(NutritionSummary.isCaloriesOnTarget(day.calories, it))) }
                            targets.proteinGrams?.let { add(yesIf(NutritionSummary.isProteinOnTarget(day.proteinGrams, it))) }
                            targets.steps?.let { target ->
                                add(yesIf(day.steps != null && NutritionSummary.isStepsOnTarget(day.steps, target)))
                            }
                        }
                    },
                ),
            ),
        )
    }

    private fun yesIf(condition: Boolean): ReportCell = if (condition) textCell(R.string.export_yes) else ReportCell.Empty
}
