package com.example.tracker.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.catalog.AddCatalogExerciseToPlan
import com.example.tracker.domain.model.CatalogExercise
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.repository.ExerciseCatalogRepository
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.ui.common.launchCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Exercise catalog screen: searches the online catalog and adds exercises from it to one plan.
 * State is exposed as [uiState]; confirmations and errors are sent once through [events].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExerciseCatalogViewModel(
    private val planId: String,
    private val catalog: ExerciseCatalogRepository,
    planRepository: PlanRepository,
    private val addToPlan: AddCatalogExerciseToPlan,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val searchAttempt = MutableStateFlow(0)
    private val selectedId = MutableStateFlow<Int?>(null)
    private val isAdding = MutableStateFlow(false)

    private val _events = Channel<ExerciseCatalogEvent>(Channel.BUFFERED)
    val events: Flow<ExerciseCatalogEvent> = _events.receiveAsFlow()

    private val search: Flow<Search> = combine(
        // Wait for a pause in typing, but show the whole catalog again at once when cleared.
        query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }.map { it.trim() }.distinctUntilChanged(),
        searchAttempt,
    ) { term, _ -> term }
        .flatMapLatest { term ->
            flow<Search> {
                emit(Search.Loading)
                emit(Search.Done(catalog.search(term)))
            }.catch { emit(Search.Failed) }
        }

    private val namesInPlan: Flow<Set<String>> = planRepository.observePlan(planId)
        .map { plan -> plan?.exercises.orEmpty().map { it.exercise.name.lowercase() }.toSet() }
        .catch { emit(emptySet()) }

    val uiState: StateFlow<ExerciseCatalogUiState> =
        combine(query, search, namesInPlan, selectedId, isAdding) { query, search, inPlan, selectedId, adding ->
            val results = when (search) {
                Search.Loading -> CatalogResultsUi.Loading
                Search.Failed -> CatalogResultsUi.Error
                is Search.Done -> CatalogResultsUi.Success(search.exercises.map { it.toUi(inPlan) })
            }
            ExerciseCatalogUiState(
                query = query,
                results = results,
                selected = (results as? CatalogResultsUi.Success)?.exercises?.firstOrNull { it.id == selectedId },
                isAdding = adding,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExerciseCatalogUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onRetry() {
        searchAttempt.update { it + 1 }
    }

    fun onSelect(exerciseId: Int) {
        selectedId.value = exerciseId
    }

    fun onDismissDetails() {
        selectedId.value = null
    }

    /** Adds the open exercise to the plan, measured by [type], and closes its details. */
    fun onAddSelected(type: ExerciseType) {
        val exercise = uiState.value.selected ?: return
        if (exercise.isInPlan || isAdding.value) return
        isAdding.value = true
        viewModelScope.launchCatching(onError = { _events.send(ExerciseCatalogEvent.ShowMessage(R.string.catalog_error_add)) }) {
            try {
                addToPlan(planId, exercise.name, type)
                selectedId.value = null
                _events.send(ExerciseCatalogEvent.ExerciseAdded(exercise.name))
            } finally {
                isAdding.value = false
            }
        }
    }

    private sealed interface Search {
        data object Loading : Search
        data object Failed : Search
        data class Done(val exercises: List<CatalogExercise>) : Search
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        internal const val SEARCH_DEBOUNCE_MILLIS = 350L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                ExerciseCatalogViewModel(
                    planId = createSavedStateHandle().toRoute<ExerciseCatalogDestination>().planId,
                    catalog = container.exerciseCatalogRepository,
                    planRepository = container.planRepository,
                    addToPlan = AddCatalogExerciseToPlan(container.exerciseRepository, container.planRepository),
                )
            }
        }
    }
}
