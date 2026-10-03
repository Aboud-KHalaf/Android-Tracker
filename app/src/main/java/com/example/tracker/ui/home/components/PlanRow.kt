package com.example.tracker.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import com.example.tracker.R
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.home.PlanItemUi

/** One plan in the grouped "Workout plans" list; opens the plan. */
@Composable
fun PlanRow(
    plan: PlanItemUi,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = { Text(plan.name) },
            supportingContent = {
                Text(pluralStringResource(R.plurals.exercise_count, plan.exerciseCount, plan.exerciseCount))
            },
            leadingContent = {
                IconAvatar(
                    icon = Icons.Outlined.FitnessCenter,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
