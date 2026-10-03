package com.example.tracker.ui.nutrition

import androidx.annotation.StringRes
import com.example.tracker.domain.model.DailyNutrition

/** One-off effects the Nutrition screen handles once. */
sealed interface NutritionEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : NutritionEvent

    /** [day] was deleted; offer to undo. */
    data class DayDeleted(val day: DailyNutrition) : NutritionEvent
}
