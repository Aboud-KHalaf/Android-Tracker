package com.example.tracker.ui.stats

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.ui.theme.TrackerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StatsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val waist = CustomStat("waist", "Waist", "cm", 1, null, StatIcon.PERCENT, StatColor.ORANGE)
    private val state = StatsUiState.Success(
        today = LocalDate.of(2026, 10, 3),
        stats = listOf(StatCardUi(waist, latest = null, changeSinceStart = null, progress = null)),
    )

    @Test
    fun longPress_editOpensTheEditor() {
        val edited = mutableListOf<String>()
        compose.setContent {
            TrackerTheme { StatsScreen(uiState = state, actions = StatsActions(onEditStat = { edited += it })) }
        }

        compose.onNodeWithText("Waist").performTouchInput { longClick() }
        compose.onNodeWithText("Edit stat").performClick()

        assertEquals(listOf("waist"), edited)
    }

    @Test
    fun longPress_deleteAsksFirst() {
        val deleted = mutableListOf<String>()
        compose.setContent {
            TrackerTheme { StatsScreen(uiState = state, actions = StatsActions(onDeleteStat = { deleted += it })) }
        }

        compose.onNodeWithText("Waist").performTouchInput { longClick() }
        compose.onNodeWithText("Delete stat").performClick()
        compose.onNodeWithText("Delete Waist?").assertExists()
        assertEquals(emptyList<String>(), deleted)
        compose.onNodeWithText("Delete").performClick()

        assertEquals(listOf("waist"), deleted)
    }
}
