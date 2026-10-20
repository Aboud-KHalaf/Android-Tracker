package com.example.tracker.ui.stats

import androidx.annotation.StringRes
import com.example.tracker.R
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatIcon

/** A ready-made stat the user can start from; everything stays editable. */
enum class StatTemplate(
    @StringRes val nameRes: Int,
    @StringRes val unitRes: Int,
    val decimals: Int,
    val icon: StatIcon,
    val color: StatColor,
) {
    WEIGHT(R.string.stat_template_weight, R.string.stat_unit_kg, 1, StatIcon.SCALE, StatColor.BLUE),
    BODY_FAT(R.string.stat_template_body_fat, R.string.stat_unit_percent, 1, StatIcon.PERCENT, StatColor.ORANGE),
    WAIST(R.string.stat_template_waist, R.string.stat_unit_cm, 1, StatIcon.RULER, StatColor.PINK),
    CALORIES(R.string.stat_template_calories, R.string.stat_unit_kcal, 0, StatIcon.FIRE, StatColor.AMBER),
    PROTEIN(R.string.stat_template_protein, R.string.stat_unit_grams, 0, StatIcon.FOOD, StatColor.GREEN),
    STEPS(R.string.stat_template_steps, R.string.stat_unit_steps, 0, StatIcon.STEPS, StatColor.TEAL),
    WATER(R.string.stat_template_water, R.string.stat_unit_liters, 1, StatIcon.WATER, StatColor.BLUE),
    SLEEP(R.string.stat_template_sleep, R.string.stat_unit_hours, 1, StatIcon.SLEEP, StatColor.PURPLE),
    RESTING_HEART_RATE(R.string.stat_template_resting_heart_rate, R.string.stat_unit_bpm, 0, StatIcon.HEART, StatColor.RED),
}
