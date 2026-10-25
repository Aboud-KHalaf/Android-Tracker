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

    @Test
    fun menu_moveDownReordersTheStats() {
        val bodyFat = waist.copy(id = "fat", name = "Body fat")
        val twoStats = state.copy(
            stats = state.stats + StatCardUi(bodyFat, latest = null, changeSinceStart = null, progress = null),
        )
        val orders = mutableListOf<List<String>>()
        compose.setContent {
            TrackerTheme { StatsScreen(uiState = twoStats, actions = StatsActions(onReorderStats = { orders += it })) }
        }

        compose.onNodeWithText("Waist").performTouchInput { longClick() }
        // The first stat can only move down.
        compose.onNodeWithText("Move up").assertDoesNotExist()
        compose.onNodeWithText("Move down").performClick()

        assertEquals(listOf(listOf("fat", "waist")), orders)
    }

    @Test
    fun longPressAndDrag_reordersWithoutOpeningTheMenuOrTheStat() {
        val bodyFat = waist.copy(id = "fat", name = "Body fat")
        val twoStats = state.copy(
            stats = state.stats + StatCardUi(bodyFat, latest = null, changeSinceStart = null, progress = null),
        )
        val orders = mutableListOf<List<String>>()
        val opened = mutableListOf<String>()
        compose.setContent {
            TrackerTheme {
                StatsScreen(
                    uiState = twoStats,
                    actions = StatsActions(onReorderStats = { orders += it }, onOpenStat = { id, _ -> opened += id }),
                )
            }
        }

        compose.onNodeWithText("Waist").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            // Well past the next card's middle.
            moveBy(androidx.compose.ui.geometry.Offset(0f, height * 1.5f))
            up()
        }

        assertEquals(listOf(listOf("fat", "waist")), orders)
        assertEquals(emptyList<String>(), opened)
        compose.onNodeWithText("Edit stat").assertDoesNotExist()
    }
}
