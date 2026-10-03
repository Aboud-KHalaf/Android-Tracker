package com.example.tracker.ui.nutrition

import com.example.tracker.R
import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.DailyNutrition
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.testing.FakeNutritionRepository
import com.example.tracker.testing.FakeSettingsRepository
import com.example.tracker.testing.MainDispatcherRule
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val nutrition = FakeNutritionRepository()
    private val settings = FakeSettingsRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 3)

    private fun createViewModel() = NutritionViewModel(nutrition, settings, time)

    private fun TestScope.collectState(viewModel: NutritionViewModel): () -> NutritionUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun (() -> NutritionUiState).success() = this() as NutritionUiState.Success

    @Test
    fun uiState_showsTodayAndThisMonthsDaysNewestFirst() = runTest {
        nutrition.put(
            DailyNutrition(today, 1850, 140),
            DailyNutrition(today.minusDays(2), 2300, 165),
            DailyNutrition(LocalDate.of(2026, 9, 30), 2000, 100), // last month: outside "this month"
        )
        settings.nutritionTargets.value = NutritionTargets(2400, 160)

        val state = collectState(createViewModel()).success()

        assertEquals(today, state.today)
        assertEquals(DailyNutrition(today, 1850, 140), state.todayEntry)
        assertEquals(LocalDate.of(2026, 10, 1), state.rangeStart)
        assertEquals(3, state.rangeDayCount)
        assertEquals(listOf(YearMonth.of(2026, 10)), state.months.map { it.month })
        assertEquals(listOf(today, today.minusDays(2)), state.months.single().days.map { it.date })
        assertEquals(2, state.summary!!.loggedDays)
        assertEquals(2075, state.summary!!.averageCalories)
        assertEquals(1, state.summary!!.proteinOnTargetDays)
    }

    @Test
    fun uiState_nothingLogged_hasNoSummaryAndNoTodayEntry() = runTest {
        val state = collectState(createViewModel()).success()

        assertNull(state.todayEntry)
        assertNull(state.summary)
        assertTrue(state.months.isEmpty())
    }

    @Test
    fun uiState_loadFailure_showsErrorAndRetryRecovers() = runTest {
        nutrition.observeError = IOException("disk")
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        assertEquals(NutritionUiState.Error, state())

        nutrition.observeError = null
        viewModel.onRetry()

        assertTrue(state() is NutritionUiState.Success)
    }

    @Test
    fun onOpenEditor_newDay_startsEmpty() = runTest {
        val viewModel = createViewModel()

        viewModel.onOpenEditor()

        assertEquals(DayEditorState(today, "", "", isExisting = false), viewModel.editor.value)
    }

    @Test
    fun onOpenEditor_loggedDay_isFilledIn() = runTest {
        nutrition.put(DailyNutrition(today.minusDays(1), 2300, 150))
        val viewModel = createViewModel()

        viewModel.onOpenEditor(today.minusDays(1))

        assertEquals(DayEditorState(today.minusDays(1), "2300", "150", isExisting = true), viewModel.editor.value)
    }

    @Test
    fun onSaveDay_savesAndClosesDialog() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorCaloriesChange("2,450")
        viewModel.onEditorProteinChange("165g")
        viewModel.onSaveDay()

        assertEquals(DailyNutrition(today, 2450, 165), nutrition.days.value[today])
        assertNull(viewModel.editor.value)
    }

    @Test
    fun onSaveDay_invalidAmounts_showsErrorsAndKeepsDialogOpen() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorCaloriesChange("")
        viewModel.onEditorProteinChange("5000")
        viewModel.onSaveDay()

        val editor = viewModel.editor.value!!
        assertTrue(editor.caloriesError)
        assertTrue(editor.proteinError)
        assertTrue(nutrition.days.value.isEmpty())

        viewModel.onEditorCaloriesChange("2000")

        assertFalse(viewModel.editor.value!!.caloriesError)
    }

    @Test
    fun onSaveDay_failure_keepsDialogOpenAndShowsMessage() = runTest {
        nutrition.writeError = IOException("disk")
        val viewModel = createViewModel()
        viewModel.onOpenEditor()
        viewModel.onEditorCaloriesChange("2000")
        viewModel.onEditorProteinChange("150")

        viewModel.onSaveDay()

        assertEquals(NutritionEvent.ShowMessage(R.string.nutrition_error_save), viewModel.events.first())
        assertFalse(viewModel.editor.value!!.isSaving)
    }

    @Test
    fun onEditorDateChange_toLoggedDay_loadsItsAmounts_andToNewDay_keepsTypedAmounts() = runTest {
        nutrition.put(DailyNutrition(today.minusDays(1), 2300, 150))
        val viewModel = createViewModel()
        viewModel.onOpenEditor()
        viewModel.onEditorCaloriesChange("1900")

        viewModel.onEditorDateChange(today.minusDays(1))

        assertEquals(DayEditorState(today.minusDays(1), "2300", "150", isExisting = true), viewModel.editor.value)

        viewModel.onEditorDateChange(today.minusDays(5))

        assertEquals(DayEditorState(today.minusDays(5), "2300", "150", isExisting = false), viewModel.editor.value)
    }

    @Test
    fun onEditorDateChange_futureDay_isIgnored() = runTest {
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onEditorDateChange(today.plusDays(1))

        assertEquals(today, viewModel.editor.value!!.date)
    }

    @Test
    fun onDeleteDay_deletesClosesAndUndoRestores() = runTest {
        val day = DailyNutrition(today, 2300, 150)
        nutrition.put(day)
        val viewModel = createViewModel()
        viewModel.onOpenEditor()

        viewModel.onDeleteDay()

        assertNull(nutrition.days.value[today])
        assertNull(viewModel.editor.value)
        assertEquals(NutritionEvent.DayDeleted(day), viewModel.events.first())

        viewModel.onUndoDelete(day)

        assertEquals(day, nutrition.days.value[today])
    }

    @Test
    fun onSaveTargets_updatesTargetsShownOnScreen() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSaveTargets(NutritionTargets(2400, null))

        assertEquals(NutritionTargets(2400, null), state.success().targets)
    }

    @Test
    fun onSaveTargets_failure_showsMessage() = runTest {
        settings.writeError = IOException("disk")
        val viewModel = createViewModel()

        viewModel.onSaveTargets(NutritionTargets(2400, 160))

        assertEquals(NutritionEvent.ShowMessage(R.string.nutrition_error_targets), viewModel.events.first())
    }
}
