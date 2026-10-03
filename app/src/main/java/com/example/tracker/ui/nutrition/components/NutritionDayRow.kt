package com.example.tracker.ui.nutrition.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.ui.common.DateTile
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.nutrition.formatAmount

/** Date tile, "2,300 kcal · 150 g protein" and the weekday. Tapping it edits the day. */
@Composable
fun NutritionDayRow(day: DailyNutrition, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(stringResource(R.string.nutrition_day_values, formatAmount(day.calories, locale), formatAmount(day.proteinGrams, locale)))
        },
        supportingContent = { Text(day.date.dayOfWeek.fullName(locale)) },
        leadingContent = { DateTile(day.date) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
