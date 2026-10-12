package com.example.tracker.ui.nutrition

import com.example.tracker.R
import com.example.tracker.domain.export.ReportCell
import com.example.tracker.domain.export.ReportText
import com.example.tracker.domain.export.numberCell
import com.example.tracker.domain.export.textCell
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NutritionReportBuilderTest {

    private val oct1 = LocalDate.of(2026, 10, 1)
    private val days = listOf(
        DailyNutrition(oct1.plusDays(1), calories = 2600, proteinGrams = 160, steps = 9000),
        DailyNutrition(oct1, calories = 2000, proteinGrams = 120),
    )

    private fun figures(targets: NutritionTargets) =
        NutritionReportBuilder.sections(days, targets).first().summary.associate { (it.label as ReportText.Res).id to it.value }

    @Test
    fun sections_withTargets_listDaysOldestFirstAndMarkHits() {
        val targets = NutritionTargets(calories = 2000, proteinGrams = 150)

        val table = NutritionReportBuilder.sections(days, targets).last().table!!

        assertEquals(6, table.headers.size)
        assertEquals(
            listOf(
                listOf(ReportCell.Date(oct1), numberCell(2000), numberCell(120), ReportCell.Empty, textCell(R.string.export_yes), ReportCell.Empty),
                listOf(ReportCell.Date(oct1.plusDays(1)), numberCell(2600), numberCell(160), numberCell(9000), ReportCell.Empty, textCell(R.string.export_yes)),
            ),
            table.rows,
        )
        assertEquals(numberCell(1), figures(targets)[R.string.export_calories_on_target_days])
        assertEquals(numberCell(2300), figures(targets)[R.string.export_average_calories])
    }

    @Test
    fun sections_withoutTargets_leaveOutTargetColumnsAndFigures() {
        val table = NutritionReportBuilder.sections(days, NutritionTargets()).last().table!!

        assertEquals(4, table.headers.size)
        assertFalse(R.string.export_calorie_target in figures(NutritionTargets()))
        assertEquals(numberCell(9000), figures(NutritionTargets())[R.string.export_average_steps])
    }
}
