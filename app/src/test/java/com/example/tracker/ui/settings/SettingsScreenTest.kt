package com.example.tracker.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.tracker.domain.workout.RestTimes
import com.example.tracker.ui.theme.TrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val restItem = "Tell me on time when a rest ends"

    private fun show(uiState: SettingsUiState, exactAlarmsAllowed: Boolean, onAllowExactAlarms: () -> Unit = {}) {
        compose.setContent {
            TrackerTheme {
                SettingsScreen(
                    uiState = uiState,
                    onBack = {},
                    onSelectThemeMode = {},
                    onSetReminder = {},
                    onSetReminderTime = {},
                    onDeleteAllData = {},
                    appVersion = "1.0",
                    exactAlarmsAllowed = exactAlarmsAllowed,
                    onAllowExactAlarms = onAllowExactAlarms,
                )
            }
        }
    }

    @Test
    fun exactAlarmsDenied_workoutSectionOffersToAllowThem() {
        var opened = 0
        show(SettingsUiState(reminderEnabled = false), exactAlarmsAllowed = false, onAllowExactAlarms = { opened++ })

        compose.onNodeWithText(restItem).performScrollTo().performClick()

        assertEquals(1, opened)
        compose.onNodeWithText("Send the reminder on time").assertDoesNotExist()
    }

    @Test
    fun exactAlarmsAllowed_noItem() {
        show(SettingsUiState(), exactAlarmsAllowed = true)

        compose.onNodeWithText(restItem).assertDoesNotExist()
    }

    @Test
    fun everyRestOff_noItem() {
        show(SettingsUiState(restTimes = RestTimes(0, 0)), exactAlarmsAllowed = false)

        compose.onNodeWithText(restItem).assertDoesNotExist()
    }
}
