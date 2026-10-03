package com.example.tracker.ui.history

import com.example.tracker.data.repository.FakeTimeProvider
import com.example.tracker.domain.model.WorkoutPlan
import com.example.tracker.domain.model.WorkoutSummary
import com.example.tracker.testing.FakePlanRepository
import com.example.tracker.testing.FakeWorkoutRepository
import com.example.tracker.testing.MainDispatcherRule
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plans = FakePlanRepository()
    private val workouts = FakeWorkoutRepository()
    private val time = FakeTimeProvider(Instant.parse("2026-10-03T09:00:00Z"))

    private fun workout(id: String, planId: String, name: String, start: String, prs: Int = 0): WorkoutSummary {
        val started = Instant.parse(start)
        return WorkoutSummary(id, planId, name, started, started.plus(Duration.ofMinutes(50)), 5, prs)
    }

    @Before
    fun setUp() {
        plans.plans.value = listOf(
            WorkoutPlan("push", "Push Day", 5, null),
            WorkoutPlan("pull", "Pull Day", 5, null),
        )
        workouts.history.value = listOf(
            workout("w3", "pull", "Pull Day", "2026-10-02T10:00:00Z", prs = 1),
            workout("w2", "push", "Push Day", "2026-09-28T10:00:00Z"),
            workout("w1", "pull", "Pull Day", "2026-09-23T10:00:00Z"),
        )
    }

    private fun TestScope.collectState(viewModel: HistoryViewModel): () -> HistoryUiState.Success {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value as HistoryUiState.Success }
    }

    private fun createViewModel() = HistoryViewModel(plans, workouts, time)

    @Test
    fun uiState_groupsAllWorkoutsByMonthNewestFirst() = runTest {
        val state = collectState(createViewModel())()

        assertEquals(listOf(null, "push", "pull"), state.filters.map { it.planId })
        assertNull(state.selectedPlanId)
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9)), state.months.map { it.month })
        assertEquals(listOf("w3"), state.months[0].workouts.map { it.id })
        assertEquals(listOf("w2", "w1"), state.months[1].workouts.map { it.id })
        assertEquals(1, state.months[0].workouts.single().personalBestCount)
    }

    @Test
    fun onSelectPlan_filtersToThatPlan() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSelectPlan("pull")

        assertEquals("pull", state().selectedPlanId)
        assertEquals(listOf("w3", "w1"), state().months.flatMap { it.workouts }.map { it.id })

        viewModel.onSelectPlan(null)
        assertEquals(3, state().months.sumOf { it.workouts.size })
    }

    @Test
    fun selectedPlanDeleted_fallsBackToAll() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)
        viewModel.onSelectPlan("push")

        plans.plans.value = plans.plans.value.filterNot { it.id == "push" }

        assertNull(state().selectedPlanId)
        assertEquals(3, state().months.sumOf { it.workouts.size })
    }

    @Test
    fun uiState_noWorkouts_hasNoMonths() = runTest {
        workouts.history.value = emptyList()

        assertTrue(collectState(createViewModel())().months.isEmpty())
    }

    @Test
    fun loadFailure_showsErrorUntilRetry() = runTest {
        workouts.historyError = IllegalStateException("disk")
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(HistoryUiState.Error, viewModel.uiState.value)

        workouts.historyError = null
        viewModel.onRetry()

        assertTrue(viewModel.uiState.value is HistoryUiState.Success)
    }
}
