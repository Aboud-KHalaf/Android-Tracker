package com.example.tracker.ui.stats.editor.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.theme.AccentColors
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.statColors

private val SelectedBorder = 2.dp

/** Every [StatIcon] as a round option; the selected one is drawn in [accent]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatIconPicker(selected: StatIcon, accent: AccentColors, onSelect: (StatIcon) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        StatIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            RoundOption(
                selected = isSelected,
                label = stringResource(icon.labelRes),
                containerColor = if (isSelected) accent.container else MaterialTheme.colorScheme.surfaceContainerHigh,
                borderColor = accent.content,
                onClick = { onSelect(icon) },
            ) {
                Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null,
                    tint = if (isSelected) accent.content else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Every [StatColor] as a swatch, with a check on the selected one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatColorPicker(selected: StatColor, onSelect: (StatColor) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        StatColor.entries.forEach { color ->
            val accent = MaterialTheme.statColors[color]
            val isSelected = color == selected
            RoundOption(
                selected = isSelected,
                label = stringResource(color.labelRes),
                containerColor = accent.container,
                borderColor = accent.content,
                onClick = { onSelect(color) },
            ) {
                if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, tint = accent.content)
            }
        }
    }
}

@Composable
private fun RoundOption(
    selected: Boolean,
    label: String,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.minTouchTarget)
            .clip(CircleShape)
            .background(containerColor)
            .then(if (selected) Modifier.border(SelectedBorder, borderColor, CircleShape) else Modifier)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@get:StringRes
private val StatIcon.labelRes: Int
    get() = when (this) {
        StatIcon.CHART -> R.string.stat_icon_chart
        StatIcon.SCALE -> R.string.stat_icon_scale
        StatIcon.RULER -> R.string.stat_icon_ruler
        StatIcon.PERCENT -> R.string.stat_icon_percent
        StatIcon.FIRE -> R.string.stat_icon_fire
        StatIcon.WATER -> R.string.stat_icon_water
        StatIcon.SLEEP -> R.string.stat_icon_sleep
        StatIcon.HEART -> R.string.stat_icon_heart
        StatIcon.BOLT -> R.string.stat_icon_bolt
        StatIcon.STEPS -> R.string.stat_icon_steps
        StatIcon.FOOD -> R.string.stat_icon_food
        StatIcon.STAR -> R.string.stat_icon_star
    }

@get:StringRes
private val StatColor.labelRes: Int
    get() = when (this) {
        StatColor.BLUE -> R.string.stat_color_blue
        StatColor.TEAL -> R.string.stat_color_teal
        StatColor.GREEN -> R.string.stat_color_green
        StatColor.AMBER -> R.string.stat_color_amber
        StatColor.ORANGE -> R.string.stat_color_orange
        StatColor.RED -> R.string.stat_color_red
        StatColor.PINK -> R.string.stat_color_pink
        StatColor.PURPLE -> R.string.stat_color_purple
    }
