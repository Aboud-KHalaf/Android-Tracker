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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.ui.common.AnimatedFab
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.NameInputDialog
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.RenameDeleteTopBar
import com.example.tracker.ui.common.ReorderHandle
import com.example.tracker.ui.common.ReorderableItem
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.common.longPressDraggable
import com.example.tracker.ui.common.mediumDate
import com.example.tracker.ui.common.moveByKey
import com.example.tracker.ui.common.rememberAfterNotificationPermission
import com.example.tracker.ui.common.rememberReorderableListState
import com.example.tracker.ui.plan.components.AddExerciseSheet
import com.example.tracker.ui.plan.components.DeletePlanDialog
import com.example.tracker.ui.plan.components.ExerciseConfigSheet
import com.example.tracker.ui.plan.components.PlanEmptyState
import com.example.tracker.ui.plan.components.PlanExerciseRow
import com.example.tracker.ui.plan.components.PlanExerciseRowActions
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
    onBrowseCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // This screen has one plan, so the token carries nothing.
    val startWorkout = rememberAfterNotificationPermission(onResult = { viewModel.onStartWorkout() })

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
            onStartWorkout = { startWorkout("") },
            onResumeWorkout = onOpenWorkout,
            onMoveUp = viewModel::onMoveUp,
            onMoveDown = viewModel::onMoveDown,
            onMoveExercise = viewModel::onMoveExercise,
            onRemoveExercise = viewModel::onRemoveExercise,
            onSupersetWithNext = viewModel::onSupersetWithNext,
            onRemoveFromSuperset = viewModel::onRemoveFromSuperset,
            onReplaceExercise = viewModel::onReplaceExercise,
            onCreateReplacement = viewModel::onCreateReplacement,
            onEditExercise = viewModel::onEditExercise,
            onSaveExerciseConfig = viewModel::onSaveExerciseConfig,
            onDismissEditor = viewModel::onDismissEditor,
            onAddExercise = viewModel::onAddExercise,
            onCreateExercise = viewModel::onCreateExercise,
            onBrowseCatalog = onBrowseCatalog,
            onRenamePlan = viewModel::onRenamePlan,
            onDeletePlan = viewModel::onDeletePlan,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

/** User actions on the training day screen. */
data class PlanActions(
    val onBack: () -> Unit = {},
    val onStartWorkout: () -> Unit = {},
    val onResumeWorkout: (workoutId: String) -> Unit = {},
    val onMoveUp: (index: Int) -> Unit = {},
    val onMoveDown: (index: Int) -> Unit = {},
    val onMoveExercise: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    val onRemoveExercise: (planExerciseId: String) -> Unit = {},
    val onSupersetWithNext: (planExerciseId: String) -> Unit = {},
    val onRemoveFromSuperset: (planExerciseId: String) -> Unit = {},
    val onReplaceExercise: (planExerciseId: String, exerciseId: String) -> Unit = { _, _ -> },
    val onCreateReplacement: (planExerciseId: String, name: String, type: ExerciseType) -> Unit = { _, _, _ -> },
    val onEditExercise: (planExerciseId: String) -> Unit = {},
    val onSaveExerciseConfig: (planExerciseId: String, config: ExerciseConfig) -> Unit = { _, _ -> },
    val onDismissEditor: () -> Unit = {},
    val onAddExercise: (exerciseId: String) -> Unit = {},
    val onCreateExercise: (name: String, type: ExerciseType) -> Unit = { _, _ -> },
    val onBrowseCatalog: () -> Unit = {},
    val onRenamePlan: (name: String) -> Unit = {},
    val onDeletePlan: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * Which dialog or sheet is open; only one at a time. REPLACE acts on the exercise in
 * `replacingId`. The targets sheet is opened by the ViewModel, since it loads data first.
 */
private enum class PlanOverlay { NONE, RENAME, DELETE, ADD_EXERCISE, REPLACE }

/** Stateless training day screen. */
@Composable
fun PlanScreen(
    uiState: PlanUiState,
    actions: PlanActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var overlay by rememberSaveable { mutableStateOf(PlanOverlay.NONE) }
    var replacingId by rememberSaveable { mutableStateOf<String?>(null) }
    val success = uiState as? PlanUiState.Success

    Scaffold(
        modifier = modifier,
        topBar = {
            RenameDeleteTopBar(
                title = success?.name.orEmpty(),
                showActions = success != null,
                optionsDescription = stringResource(R.string.plan_options),
                renameLabel = stringResource(R.string.plan_rename),
                deleteLabel = stringResource(R.string.plan_delete),
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
                    onReplaceClick = { planExerciseId ->
                        replacingId = planExerciseId
                        overlay = PlanOverlay.REPLACE
                    },
                    contentPadding = innerPadding,
                )
            }
        }
    }

    if (success != null) {
        val replacing = success.exercises.firstOrNull { it.id == replacingId }
        PlanOverlays(overlay, success, replacing, actions, onDismiss = { overlay = PlanOverlay.NONE })
        success.editor?.let { editor ->
            ExerciseConfigSheet(
                editor = editor,
                onSave = { config -> actions.onSaveExerciseConfig(editor.planExerciseId, config) },
                onDismiss = actions.onDismissEditor,
            )
        }
    }
}

@Composable
private fun PlanOverlays(
    overlay: PlanOverlay,
    state: PlanUiState.Success,
    replacing: PlanExerciseUi?,
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
            onBrowseCatalog = actions.onBrowseCatalog,
            onDismiss = onDismiss,
        )

        // The exercise may be removed while the sheet is open; then just close it.
        PlanOverlay.REPLACE -> if (replacing == null) {
            LaunchedEffect(Unit) { onDismiss() }
        } else {
            AddExerciseSheet(
                options = state.availableExercises,
                onAdd = { exerciseId -> actions.onReplaceExercise(replacing.id, exerciseId) },
                onCreate = { name, type -> actions.onCreateReplacement(replacing.id, name, type) },
                onBrowseCatalog = null,
                onDismiss = onDismiss,
                title = stringResource(R.string.replace_exercise_title, replacing.name),
                emptyText = stringResource(R.string.replace_exercise_library_empty),
                createLabel = stringResource(R.string.replace_exercise_confirm),
            )
        }
    }
}

@Composable
private fun PlanContent(
    state: PlanUiState.Success,
    actions: PlanActions,
    onAddExerciseClick: () -> Unit,
    onReplaceClick: (planExerciseId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    // The order shown while dragging; saved on drop, then replaced by the stored order.
    var order by remember { mutableStateOf(state.exercises) }
    var storedOrder by remember { mutableStateOf(state.exercises) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableListState(
        listState = listState,
        canMove = { key -> order.any { it.id == key } },
        onMove = { from, to -> order = order.moveByKey(from, to) { it.id } },
        onDrop = { key ->
            // If exercises were added or removed during the drag, the indices no longer match
            // the stored list; keep the stored order rather than move the wrong exercise.
            if (order.map { it.id }.toSet() == state.exercises.map { it.id }.toSet()) {
                val from = state.exercises.indexOfFirst { it.id == key }
                val to = order.indexOfFirst { it.id == key }
                if (from != to) actions.onMoveExercise(from, to)
            }
        },
    )
    // Show the stored order when it changes, but never mid-drag, which would drop the drag.
    if (state.exercises != storedOrder && reorderState.draggingKey == null) {
        storedOrder = state.exercises
        order = state.exercises
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
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
            items(order, key = { it.id }) { exercise ->
                ReorderableItem(reorderState, key = exercise.id) { isDragging ->
                    PlanExerciseRow(
                        exercise = exercise,
                        isDragging = isDragging,
                        dragHandle = {
                            ReorderHandle(
                                state = reorderState,
                                key = exercise.id,
                                contentDescription = stringResource(R.string.plan_drag_handle, exercise.name),
                            )
                        },
                        actions = PlanExerciseRowActions(
                            onEdit = { actions.onEditExercise(exercise.id) },
                            onReplace = { onReplaceClick(exercise.id) },
                            // Indices of the stored order: a drag whose save failed may still show.
                            onMoveUp = { actions.onMoveUp(state.exercises.indexOfFirst { it.id == exercise.id }) },
                            onMoveDown = { actions.onMoveDown(state.exercises.indexOfFirst { it.id == exercise.id }) },
                            onRemove = { actions.onRemoveExercise(exercise.id) },
                            onSupersetWithNext = { actions.onSupersetWithNext(exercise.id) },
                            onRemoveFromSuperset = { actions.onRemoveFromSuperset(exercise.id) },
                        ),
                        modifier = Modifier.longPressDraggable(reorderState, exercise.id),
                    )
                }
            }
            if (state.exercises.isEmpty()) {
                item(key = "empty") { PlanEmptyState() }
                item(key = "browse") {
                    BrowseCatalogButton(
                        onClick = actions.onBrowseCatalog,
                        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                    )
                }
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
    val locale = currentLocale()
    val separator = stringResource(R.string.separator_dot)
    val weekday = state.dayOfWeek?.fullName(locale) ?: stringResource(R.string.program_any_day)
    val lastDone = state.lastDoneOn
        ?.let { stringResource(R.string.home_last_done, it.mediumDate(locale)) }
        ?: stringResource(R.string.home_never_done)
    Text(
        text = weekday + separator + pluralStringResource(R.plurals.exercise_count, count, count) +
            separator + lastDone,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The quickest way to fill a new plan, so it leads while the plan is empty. */
@Composable
private fun BrowseCatalogButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouchTarget),
    ) {
        Icon(Icons.Outlined.TravelExplore, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.add_exercise_browse))
    }
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
