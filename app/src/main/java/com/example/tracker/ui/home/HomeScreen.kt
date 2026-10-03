package com.example.tracker.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.GroupedListGap
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.NameInputDialog
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.WorkoutSummaryRow
import com.example.tracker.ui.common.WorkoutSummaryUi
import com.example.tracker.ui.common.groupedListItemShape
import com.example.tracker.ui.home.components.HomeHeader
import com.example.tracker.ui.home.components.PlanRow
import com.example.tracker.ui.home.components.UpNextCard
import com.example.tracker.ui.home.components.WeekCard
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing

/**
 * Home destination: connects [HomeViewModel] to [HomeScreen] and turns its events into
 * navigation and snackbars.
 */
@Composable
fun HomeRoute(
    onOpenWorkout: (workoutId: String) -> Unit,
    onOpenPlan: (planId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.OpenWorkout -> onOpenWorkout(event.workoutId)
            is HomeEvent.OpenPlan -> onOpenPlan(event.planId)
            is HomeEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
    }

    HomeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = HomeActions(
            onStartWorkout = viewModel::onStartWorkout,
            onResumeWorkout = onOpenWorkout,
            onCreatePlan = viewModel::onCreatePlan,
            onOpenPlan = onOpenPlan,
            onOpenHistory = onOpenHistory,
            onOpenExerciseProgress = onOpenExerciseProgress,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

/** User actions on Home, grouped so the screen's signature stays readable. */
data class HomeActions(
    val onStartWorkout: (planId: String) -> Unit = {},
    val onResumeWorkout: (workoutId: String) -> Unit = {},
    val onCreatePlan: (name: String) -> Unit = {},
    val onOpenPlan: (planId: String) -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onOpenExerciseProgress: (exerciseId: String) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** Stateless Home screen. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showCreatePlan by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            HomeUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            HomeUiState.Error -> ErrorContent(
                message = stringResource(R.string.home_error_load),
                onRetry = actions.onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Success -> HomeContent(
                state = uiState,
                actions = actions,
                onCreatePlanClick = { showCreatePlan = true },
                contentPadding = innerPadding,
            )
        }
    }

    if (showCreatePlan) {
        NameInputDialog(
            title = stringResource(R.string.create_plan_title),
            label = stringResource(R.string.plan_name_label),
            confirmLabel = stringResource(R.string.create_plan_confirm),
            onConfirm = { name ->
                showCreatePlan = false
                actions.onCreatePlan(name)
            },
            onDismiss = { showCreatePlan = false },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    actions: HomeActions,
    onCreatePlanClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.lg,
                end = spacing.lg,
                top = contentPadding.calculateTopPadding() + spacing.xl,
                bottom = contentPadding.calculateBottomPadding() + spacing.xl,
            ),
        ) {
            item(key = "header") {
                HomeHeader(today = state.today, modifier = Modifier.padding(bottom = spacing.lg))
            }
            item(key = "up-next") {
                UpNextCard(
                    upNext = state.upNext,
                    isStartingWorkout = state.isStartingWorkout,
                    onStartWorkout = actions.onStartWorkout,
                    onResumeWorkout = actions.onResumeWorkout,
                    onCreatePlan = onCreatePlanClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "week") {
                WeekCard(
                    week = state.week,
                    onOpenExerciseProgress = actions.onOpenExerciseProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xl),
                )
            }
            plansSection(state.plans, onOpenPlan = actions.onOpenPlan, onCreatePlan = onCreatePlanClick)
            recentWorkoutsSection(state.recentWorkouts, onSeeAll = actions.onOpenHistory)
        }
    }
}

private fun LazyListScope.plansSection(
    plans: List<PlanItemUi>,
    onOpenPlan: (String) -> Unit,
    onCreatePlan: () -> Unit,
) {
    item(key = "plans-header") {
        SectionHeader(
            title = stringResource(R.string.home_workout_plans),
            actionLabel = stringResource(R.string.home_new_plan),
            actionIcon = Icons.Outlined.Add,
            onAction = onCreatePlan,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xl, bottom = MaterialTheme.spacing.sm),
        )
    }
    if (plans.isEmpty()) {
        item(key = "plans-empty") { EmptySectionText(stringResource(R.string.home_no_plans)) }
    }
    itemsIndexed(plans, key = { _, plan -> "plan-${plan.id}" }) { index, plan ->
        PlanRow(
            plan = plan,
            shape = groupedListItemShape(index, plans.size),
            onClick = { onOpenPlan(plan.id) },
            modifier = if (index == 0) Modifier else Modifier.padding(top = GroupedListGap),
        )
    }
}

private fun LazyListScope.recentWorkoutsSection(
    workouts: List<WorkoutSummaryUi>,
    onSeeAll: () -> Unit,
) {
    item(key = "recent-header") {
        SectionHeader(
            title = stringResource(R.string.home_recent_workouts),
            actionLabel = stringResource(R.string.home_see_all).takeIf { workouts.isNotEmpty() },
            onAction = onSeeAll,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xl, bottom = MaterialTheme.spacing.sm),
        )
    }
    if (workouts.isEmpty()) {
        item(key = "recent-empty") { EmptySectionText(stringResource(R.string.home_no_recent)) }
    }
    itemsIndexed(workouts, key = { _, workout -> "workout-${workout.id}" }) { index, workout ->
        Surface(
            shape = groupedListItemShape(index, workouts.size),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = if (index == 0) Modifier else Modifier.padding(top = GroupedListGap),
        ) {
            WorkoutSummaryRow(workout)
        }
    }
}

@Composable
private fun EmptySectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
