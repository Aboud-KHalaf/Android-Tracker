package com.example.tracker.ui.settings.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.tracker.R
import com.example.tracker.domain.model.ThemeMode

/** System default / Light / Dark as a radio group. */
@Composable
fun ThemeModeOptions(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            ListItem(
                headlineContent = { Text(stringResource(mode.labelRes)) },
                leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                modifier = Modifier.selectable(
                    selected = isSelected,
                    onClick = { onSelect(mode) },
                    role = Role.RadioButton,
                ),
            )
        }
    }
}

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
