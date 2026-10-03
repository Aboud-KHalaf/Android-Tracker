package com.example.tracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.repository.PlanRepository
import com.example.tracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** History: finished workouts by month, optionally filtered to one plan. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    time: TimeProvider,
) : ViewModel() {

    private val mapper = HistoryStateMapper(time.zone())
    private val loadAttempt = MutableStateFlow(0)
    private val selectedPlanId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(loadAttempt, selectedPlanId) { _, planId -> planId }
        .flatMapLatest { requested ->
            planRepository.observePlans().flatMapLatest { plans ->
                // A deleted plan's filter falls back to all workouts.
                val planId = requested?.takeIf { id -> plans.any { it.id == id } }
                workoutRepository.observeHistory(planId).map { workouts ->
                    mapper.map(plans, planId, workouts) as HistoryUiState
                }
            }
                .onStart { emit(HistoryUiState.Loading) }
                .catch { emit(HistoryUiState.Error) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState.Loading)

    /** Shows only workouts from [planId], or all workouts when null. */
    fun onSelectPlan(planId: String?) {
        selectedPlanId.value = planId
    }

    fun onRetry() {
        loadAttempt.update { it + 1 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TrackerApplication).container
                HistoryViewModel(container.planRepository, container.workoutRepository, container.time)
            }
        }
    }
}
