package com.example.tracker.ui.catalog

import com.example.tracker.R
import com.example.tracker.domain.catalog.AddCatalogExerciseToPlan
import com.example.tracker.domain.model.CatalogExercise
import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.PlanExercise
import com.example.tracker.testing.FakeExerciseCatalogRepository
import com.example.tracker.testing.FakeExerciseRepository
import com.example.tracker.testing.FakePlanRepository
import com.example.tracker.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseCatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalog = FakeExerciseCatalogRepository()
    private val plans = FakePlanRepository()
    private val exercises = FakeExerciseRepository()

    private val bench = catalogExercise(73, "Bench Press", "Chest")
    private val plank = catalogExercise(238, "Plank", "Abs")

    @Before
    fun setUp() {
        catalog.results = listOf(bench, plank)
        val inPlan = Exercise("bench", "bench press", ExerciseType.WEIGHT_REPS)
        plans.planDetails.value = mapOf(
            "push" to PlanDetails("push", "Push Day", listOf(PlanExercise("pe-bench", inPlan, 0, targetSets = 3))),
        )
    }

    private fun catalogExercise(id: Int, name: String, category: String) = CatalogExercise(
        id = id,
        name = name,
        category = category,
        primaryMuscles = emptyList(),
        secondaryMuscles = emptyList(),
        equipment = emptyList(),
        description = "",
        imageUrl = null,
    )

    private fun createViewModel() =
        ExerciseCatalogViewModel("push", catalog, plans, AddCatalogExerciseToPlan(exercises, plans))

    private fun TestScope.collectState(viewModel: ExerciseCatalogViewModel): () -> ExerciseCatalogUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return { viewModel.uiState.value }
    }

    private fun ExerciseCatalogUiState.exercises() = (results as CatalogResultsUi.Success).exercises

    @Test
    fun uiState_initiallyBrowsesTheWholeCatalogMarkingExercisesInThePlan() = runTest {
        val state = collectState(createViewModel()).invoke()

        assertEquals(listOf(""), catalog.queries)
        assertEquals(listOf("Bench Press" to true, "Plank" to false), state.exercises().map { it.name to it.isInPlan })
        assertEquals(ExerciseType.DURATION, state.exercises().last().suggestedType)
    }

    @Test
    fun onQueryChange_searchesOnceTypingPauses() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onQueryChange("ben")
        advanceTimeBy(100)
        viewModel.onQueryChange("bench ")
        assertEquals("bench ", state().query)
        assertEquals(listOf(""), catalog.queries)

        advanceTimeBy(ExerciseCatalogViewModel.SEARCH_DEBOUNCE_MILLIS)
        runCurrent()
        assertEquals(listOf("", "bench"), catalog.queries)
    }

    @Test
    fun searchFailure_showsErrorAndRetrySearchesAgain() = runTest {
        catalog.error = IOException("offline")
        val viewModel = createViewModel()
        val state = collectState(viewModel)
        assertEquals(CatalogResultsUi.Error, state().results)

        catalog.error = null
        viewModel.onRetry()

        assertEquals(2, state().exercises().size)
        assertEquals(listOf("", ""), catalog.queries)
    }

    @Test
    fun onSelect_opensDetailsAndDismissClosesThem() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)

        viewModel.onSelect(238)
        assertEquals("Plank", state().selected?.name)

        viewModel.onDismissDetails()
        assertNull(state().selected)
    }

    @Test
    fun onAddSelected_addsWithChosenTypeClosesDetailsAndConfirms() = runTest {
        val viewModel = createViewModel()
        val state = collectState(viewModel)
        viewModel.onSelect(238)

        viewModel.onAddSelected(ExerciseType.WEIGHT_REPS)

        assertEquals(listOf("Plank" to ExerciseType.WEIGHT_REPS), exercises.createdExercises)
        assertEquals(listOf("add push exercise-1"), plans.writes)
        assertNull(state().selected)
        assertFalse(state().isAdding)
        assertEquals(ExerciseCatalogEvent.ExerciseAdded("Plank"), viewModel.events.first())
    }

    @Test
    fun onAddSelected_exerciseAlreadyInPlan_doesNothing() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)
        viewModel.onSelect(73)

        viewModel.onAddSelected(ExerciseType.WEIGHT_REPS)

        assertTrue(plans.writes.isEmpty())
    }

    @Test
    fun onAddSelected_failure_showsMessageAndKeepsDetailsOpen() = runTest {
        plans.writeError = IllegalStateException("disk full")
        val viewModel = createViewModel()
        val state = collectState(viewModel)
        viewModel.onSelect(238)

        viewModel.onAddSelected(ExerciseType.DURATION)

        assertEquals(ExerciseCatalogEvent.ShowMessage(R.string.catalog_error_add), viewModel.events.first())
        assertEquals("Plank", state().selected?.name)
        assertFalse(state().isAdding)
    }
}
