package com.example.tracker.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.shortName
import com.example.tracker.ui.home.ProgramItemUi

/** One program in the grouped "Programs" list, with its training days; opens the program. */
@Composable
fun ProgramRow(
    program: ProgramItemUi,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val separator = stringResource(R.string.separator_dot)
    val days = program.trainingDays.joinToString(separator) { it.shortName(locale) }
        .ifEmpty { stringResource(R.string.program_no_training_days) }
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = { Text(program.name) },
            supportingContent = { Text(days) },
            overlineContent = if (program.isActive) {
                { Text(stringResource(R.string.program_active), color = MaterialTheme.colorScheme.primary) }
            } else {
                null
            },
            leadingContent = {
                IconAvatar(
                    icon = Icons.Outlined.CalendarMonth,
                    containerColor = if (program.isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (program.isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
