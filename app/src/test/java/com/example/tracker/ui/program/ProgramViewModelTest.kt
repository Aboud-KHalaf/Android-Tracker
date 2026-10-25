package com.example.tracker.ui.program

import com.example.tracker.R
import com.example.tracker.domain.model.Program
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.testing.FakePlanRepository
import com.example.tracker.testing.FakeProgramRepository
import com.example.tracker.testing.MainDispatcherRule
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgramViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val programs = FakeProgramRepository()
    private val plans = FakePlanRepository()

    private fun createViewModel() = ProgramViewModel("ppl", programs, plans)

    private fun TestScope.collectState(viewModel: ProgramViewModel): () -> ProgramUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun seed(isActive: Boolean = true) {
        programs.programs.value = listOf(
            Program(
                id = "ppl",
                name = "PPL",
                isActive = isActive,
                days = listOf(
                    WorkoutPlan("push", "Push", 4, null, programId = "ppl", dayOfWeek = DayOfWeek.MONDAY),
                    WorkoutPlan("extra", "Arms", 0, null, programId = "ppl", dayOfWeek = null),
                ),
            ),
        )
    }

    @Test
    fun uiState_showsScheduleAndDays() = runTest {
        seed()
        val state = collectState(createViewModel())

        val success = state() as ProgramUiState.Success
        assertEquals("PPL", success.name)
        assertTrue(success.isActive)
        assertEquals(setOf(DayOfWeek.MONDAY), success.trainingDays)
        assertEquals(
            listOf(ProgramDayUi("push", "Push", DayOfWeek.MONDAY, 4), ProgramDayUi("extra", "Arms", null, 0)),
            success.days,
        )
    }

    @Test
    fun uiState_missingProgram_isNotFound() = runTest {
        val state = collectState(createViewModel())

        assertEquals(ProgramUiState.NotFound, state())
    }

    @Test
    fun dayActions_writeThroughTheRepositories() = runTest {
        seed()
        val viewModel = createViewModel()

        viewModel.onAddDay(null, " Legs ")
        viewModel.onScheduleDay("extra", DayOfWeek.SUNDAY)
        viewModel.onReorderDays(listOf("extra", "push"))

        assertEquals(listOf("add ppl null Legs", "schedule extra SUNDAY", "reorder ppl [extra, push]"), programs.writes)
    }

    @Test
    fun toggleFreeWeekday_addsADay_andARepeatTapIsIgnoredUntilItShows() = runTest {
        seed()
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onToggleWeekday(DayOfWeek.FRIDAY, "Friday")
        viewModel.onToggleWeekday(DayOfWeek.FRIDAY, "Friday")

        assertEquals(listOf("add ppl FRIDAY Friday"), programs.writes)
    }

    @Test
    fun toggleWeekdayWithExercises_asksBeforeRemoving() = runTest {
        seed()
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onToggleWeekday(DayOfWeek.MONDAY, "Monday")
        assertEquals("push", (state() as ProgramUiState.Success).removalToConfirm?.id)
        assertTrue(plans.writes.isEmpty())

        viewModel.onConfirmRemoveDay()
        assertEquals(listOf("delete push"), plans.writes)
        assertEquals(null, (state() as ProgramUiState.Success).removalToConfirm)
    }

    @Test
    fun cancellingTheRemoval_keepsTheDay() = runTest {
        seed()
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onRequestRemoveDay("extra")
        viewModel.onCancelRemoveDay()

        assertEquals(null, (state() as ProgramUiState.Success).removalToConfirm)
        assertTrue(plans.writes.isEmpty())
    }

    @Test
    fun toggleEmptyScheduledDay_removesItRightAway() = runTest {
        programs.programs.value = listOf(
            Program("ppl", "PPL", true, listOf(WorkoutPlan("push", "Push", 0, null, "ppl", DayOfWeek.MONDAY))),
        )
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onToggleWeekday(DayOfWeek.MONDAY, "Monday")

        assertEquals(listOf("delete push"), plans.writes)
    }

    @Test
    fun onAddDay_blankName_isIgnored() = runTest {
        createViewModel().onAddDay(null, "  ")

        assertTrue(programs.writes.isEmpty())
    }

    @Test
    fun onMakeActive_andRename_updateTheProgram() = runTest {
        seed(isActive = false)
        val viewModel = createViewModel()

        viewModel.onMakeActive()
        viewModel.onRenameProgram("  Upper/Lower ")

        assertEquals(listOf("active ppl", "rename ppl Upper/Lower"), programs.writes)
    }

    @Test
    fun onDeleteProgram_leavesTheScreen() = runTest {
        seed()
        val viewModel = createViewModel()

        viewModel.onDeleteProgram()

        assertEquals(listOf("delete ppl"), programs.writes)
        assertEquals(ProgramEvent.ProgramDeleted, viewModel.events.first())
    }

    @Test
    fun failedUpdate_showsMessage() = runTest {
        seed()
        programs.writeError = IllegalArgumentException("taken")
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onToggleWeekday(DayOfWeek.FRIDAY, "Friday")

        assertEquals(ProgramEvent.ShowMessage(R.string.program_error_update), viewModel.events.first())
    }
}
