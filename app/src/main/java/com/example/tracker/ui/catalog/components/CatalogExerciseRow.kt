package com.example.tracker.ui.catalog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.ui.catalog.CatalogExerciseUi

private val ThumbnailSize = 56.dp

/** A catalog search result: picture, name, and category with equipment; opens its details. */
@Composable
fun CatalogExerciseRow(
    exercise: CatalogExerciseUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val separator = stringResource(R.string.separator_dot)
    ListItem(
        headlineContent = { Text(exercise.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val details = listOfNotNull(exercise.category, exercise.equipment.joinToString().ifEmpty { null })
            if (details.isNotEmpty()) {
                Text(details.joinToString(separator), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        leadingContent = {
            CatalogExerciseImage(
                imageUrl = exercise.imageUrl,
                type = exercise.suggestedType,
                contentDescription = null,
                modifier = Modifier.size(ThumbnailSize),
            )
        },
        trailingContent = if (exercise.isInPlan) {
            {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.catalog_in_plan),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}
