package com.example.tracker.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.ui.theme.TrackerTheme

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    TrackerTheme {
        SettingsScreen(
            uiState = SettingsUiState(themeMode = ThemeMode.DARK),
            onBack = {},
            onSelectThemeMode = {},
            onSetNutritionReminder = {},
            onSetNutritionReminderTime = {},
            onDeleteAllData = {},
            appVersion = "1.0",
        )
    }
}
