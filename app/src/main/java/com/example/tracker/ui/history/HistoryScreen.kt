package com.example.tracker.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MonthHeader
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.WorkoutSummaryRow
import com.example.tracker.ui.history.components.ConsistencyCard
import com.example.tracker.ui.history.components.PlanFilterChips
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/** History destination: connects [HistoryViewModel] to [HistoryScreen]. */
@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWorkout: (workoutId: String) -> Unit = {},
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onSelectPlan = viewModel::onSelectPlan,
        onRetry = viewModel::onRetry,
        onBack = onBack,
        onOpenWorkout = onOpenWorkout,
        modifier = modifier,
    )
}

/** Stateless History screen, opened from the Home and Progress headers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSelectPlan: (planId: String?) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWorkout: (workoutId: String) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                HistoryUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                HistoryUiState.Error -> ErrorContent(
                    message = stringResource(R.string.history_error_load),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                is HistoryUiState.Success -> HistoryContent(state, onSelectPlan, onOpenWorkout, innerPadding)
            }
        }
    }
}

@Composable
private fun HistoryContent(
    state: HistoryUiState.Success,
    onSelectPlan: (String?) -> Unit,
    onOpenWorkout: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + spacing.lg,
            ),
        ) {
            item(key = "consistency") {
                ConsistencyCard(
                    consistency = state.consistency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.lg),
                )
            }
            item(key = "filters") {
                PlanFilterChips(
                    filters = state.filters,
                    selectedPlanId = state.selectedPlanId,
                    onSelect = onSelectPlan,
                    modifier = Modifier.padding(bottom = spacing.sm),
                )
            }
            if (state.months.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(
                            if (state.selectedPlanId == null) R.string.history_empty_all else R.string.history_empty_plan,
                        ),
                    )
                }
            }
            state.months.forEach { group ->
                stickyHeader(key = "month-${group.month}") {
                    val count = group.workouts.size
                    MonthHeader(
                        month = group.month,
                        countText = pluralStringResource(R.plurals.workout_count, count, count),
                    )
                }
                items(group.workouts, key = { it.id }) { workout ->
                    WorkoutSummaryRow(workout, Modifier.animateItem(), onClick = { onOpenWorkout(workout.id) })
                }
            }
        }
    }
}
