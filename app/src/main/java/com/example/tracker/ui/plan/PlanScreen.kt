package com.example.tracker.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.AnimatedFab
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.NameInputDialog
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.mediumDate
import com.example.tracker.ui.plan.components.AddExerciseSheet
import com.example.tracker.ui.plan.components.DeletePlanDialog
import com.example.tracker.ui.plan.components.PlanEmptyState
import com.example.tracker.ui.plan.components.PlanExerciseRow
import com.example.tracker.ui.plan.components.PlanTopBar
import com.example.tracker.ui.plan.components.WorkoutFab
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch

/** Space below the list so the last row can scroll clear of the floating button. */
private val FabClearance = 88.dp

/**
 * Workout plan destination: connects [PlanViewModel] to [PlanScreen] and turns its events
 * into navigation and snackbars.
 */
@Composable
fun PlanRoute(
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PlanEvent.OpenWorkout -> onOpenWorkout(event.workoutId)
            PlanEvent.PlanDeleted -> onBack()
            is PlanEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            // Launched so a second removal replaces this snackbar instead of waiting for it.
            is PlanEvent.ExerciseRemoved -> scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.plan_exercise_removed, event.exerciseName),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onUndoRemoveExercise(event.planExerciseId)
                }
            }
        }
    }

    PlanScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = PlanActions(
            onBack = onBack,
            onStartWorkout = viewModel::onStartWorkout,
            onResumeWorkout = onOpenWorkout,
            onMoveUp = viewModel::onMoveUp,
            onMoveDown = viewModel::onMoveDown,
            onRemoveExercise = viewModel::onRemoveExercise,
            onAddExercise = viewModel::onAddExercise,
            onCreateExercise = viewModel::onCreateExercise,
            onRenamePlan = viewModel::onRenamePlan,
            onDeletePlan = viewModel::onDeletePlan,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

/** User actions on the Workout plan screen. */
data class PlanActions(
    val onBack: () -> Unit = {},
    val onStartWorkout: () -> Unit = {},
    val onResumeWorkout: (workoutId: String) -> Unit = {},
    val onMoveUp: (index: Int) -> Unit = {},
    val onMoveDown: (index: Int) -> Unit = {},
    val onRemoveExercise: (planExerciseId: String) -> Unit = {},
    val onAddExercise: (exerciseId: String) -> Unit = {},
    val onCreateExercise: (name: String, type: ExerciseType) -> Unit = { _, _ -> },
    val onRenamePlan: (name: String) -> Unit = {},
    val onDeletePlan: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** Which dialog or sheet is open; only one at a time. */
private enum class PlanOverlay { NONE, RENAME, DELETE, ADD_EXERCISE }

/** Stateless Workout plan screen. */
@Composable
fun PlanScreen(
    uiState: PlanUiState,
    actions: PlanActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var overlay by rememberSaveable { mutableStateOf(PlanOverlay.NONE) }
    val success = uiState as? PlanUiState.Success

    Scaffold(
        modifier = modifier,
        topBar = {
            PlanTopBar(
                title = success?.name.orEmpty(),
                showPlanActions = success != null,
                onBack = actions.onBack,
                onRename = { overlay = PlanOverlay.RENAME },
                onDelete = { overlay = PlanOverlay.DELETE },
            )
        },
        floatingActionButton = {
            if (success != null) {
                AnimatedFab {
                    WorkoutFab(
                        action = success.workoutAction,
                        isStartingWorkout = success.isStartingWorkout,
                        onStart = actions.onStartWorkout,
                        onResume = actions.onResumeWorkout,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                PlanUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                PlanUiState.Error -> ErrorContent(
                    message = stringResource(R.string.plan_error_load),
                    onRetry = actions.onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                PlanUiState.NotFound -> MessageContent(stringResource(R.string.plan_not_found), Modifier.padding(innerPadding))
                is PlanUiState.Success -> PlanContent(
                    state = state,
                    actions = actions,
                    onAddExerciseClick = { overlay = PlanOverlay.ADD_EXERCISE },
                    contentPadding = innerPadding,
                )
            }
        }
    }

    if (success != null) {
        PlanOverlays(overlay, success, actions, onDismiss = { overlay = PlanOverlay.NONE })
    }
}

@Composable
private fun PlanOverlays(
    overlay: PlanOverlay,
    state: PlanUiState.Success,
    actions: PlanActions,
    onDismiss: () -> Unit,
) {
    when (overlay) {
        PlanOverlay.NONE -> Unit
        PlanOverlay.RENAME -> NameInputDialog(
            title = stringResource(R.string.plan_rename),
            label = stringResource(R.string.plan_name_label),
            confirmLabel = stringResource(R.string.plan_rename_confirm),
            initialName = state.name,
            onConfirm = { name ->
                onDismiss()
                actions.onRenamePlan(name)
            },
            onDismiss = onDismiss,
        )

        PlanOverlay.DELETE -> DeletePlanDialog(
            planName = state.name,
            onConfirm = {
                onDismiss()
                actions.onDeletePlan()
            },
            onDismiss = onDismiss,
        )

        PlanOverlay.ADD_EXERCISE -> AddExerciseSheet(
            options = state.availableExercises,
            onAdd = actions.onAddExercise,
            onCreate = actions.onCreateExercise,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun PlanContent(
    state: PlanUiState.Success,
    actions: PlanActions,
    onAddExerciseClick: () -> Unit,
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
                bottom = contentPadding.calculateBottomPadding() + FabClearance,
            ),
        ) {
            item(key = "summary") {
                PlanSummary(state, Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.sm))
            }
            itemsIndexed(state.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                PlanExerciseRow(
                    exercise = exercise,
                    onMoveUp = { actions.onMoveUp(index) },
                    onMoveDown = { actions.onMoveDown(index) },
                    onRemove = { actions.onRemoveExercise(exercise.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (state.exercises.isEmpty()) {
                item(key = "empty") { PlanEmptyState() }
            }
            item(key = "add") {
                AddExerciseButton(
                    onClick = onAddExerciseClick,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun PlanSummary(state: PlanUiState.Success, modifier: Modifier = Modifier) {
    val count = state.exercises.size
    val lastDone = state.lastDoneOn
        ?.let { stringResource(R.string.home_last_done, it.mediumDate(currentLocale())) }
        ?: stringResource(R.string.home_never_done)
    Text(
        text = pluralStringResource(R.plurals.exercise_count, count, count) +
            stringResource(R.string.separator_dot) + lastDone,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouchTarget),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.plan_add_exercise))
    }
}
