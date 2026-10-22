package com.example.tracker.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.sharedAxisX
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.workout.components.ActiveSetCard
import com.example.tracker.ui.workout.components.AllSetsDoneBanner
import com.example.tracker.ui.workout.components.DiscardWorkoutDialog
import com.example.tracker.ui.workout.components.DoneSetRow
import com.example.tracker.ui.workout.components.DurationEditorActions
import com.example.tracker.ui.workout.components.ExerciseHeader
import com.example.tracker.ui.workout.components.FinishWorkoutDialog
import com.example.tracker.ui.workout.components.KeepScreenOn
import com.example.tracker.ui.workout.components.RestTimerActions
import com.example.tracker.ui.workout.components.RestTimerBar
import com.example.tracker.ui.workout.components.UpcomingSetRow
import com.example.tracker.ui.workout.components.WeightRepsEditorActions
import com.example.tracker.ui.workout.components.WorkoutFooter
import com.example.tracker.ui.workout.components.WorkoutTopBar
import kotlinx.coroutines.launch

/**
 * Active workout destination: connects [ActiveWorkoutViewModel] to [ActiveWorkoutScreen]
 * and turns its events into navigation and snackbars.
 */
@Composable
fun ActiveWorkoutRoute(
    onMinimize: () -> Unit,
    onWorkoutEnded: () -> Unit,
    onOpenExerciseProgress: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = viewModel(factory = ActiveWorkoutViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ActiveWorkoutEvent.WorkoutEnded -> onWorkoutEnded()
            is ActiveWorkoutEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            // Launched so completing the next set replaces this snackbar instead of waiting.
            is ActiveWorkoutEvent.SetCompleted -> scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.workout_set_saved, event.setNumber),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndoCompleteSet(event.setId)
            }
        }
    }

    ActiveWorkoutScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = ActiveWorkoutActions(
            onMinimize = onMinimize,
            onFinish = viewModel::onFinishWorkout,
            onDiscard = viewModel::onDiscardWorkout,
            onOpenExerciseProgress = onOpenExerciseProgress,
            weightReps = WeightRepsEditorActions(
                onWeightChange = viewModel::onWeightTextChange,
                onRepsChange = viewModel::onRepsTextChange,
                onWeightStep = viewModel::onWeightStep,
                onRepsStep = viewModel::onRepsStep,
            ),
            duration = DurationEditorActions(
                onToggle = viewModel::onToggleTimer,
                onAdjust = viewModel::onAdjustHold,
            ),
            onCompleteSet = viewModel::onCompleteSet,
            onRemoveActiveSet = viewModel::onRemoveActiveSet,
            onSelectSet = viewModel::onSelectSet,
            onAddSet = viewModel::onAddSet,
            rest = RestTimerActions(onAdjust = viewModel::onAdjustRest, onEnd = viewModel::onEndRest),
            onPreviousExercise = viewModel::onPreviousExercise,
            onNextExercise = viewModel::onNextExercise,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

/** User actions on the Active workout screen. */
data class ActiveWorkoutActions(
    val onMinimize: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onOpenExerciseProgress: (exerciseId: String) -> Unit = {},
    val weightReps: WeightRepsEditorActions = WeightRepsEditorActions(),
    val duration: DurationEditorActions = DurationEditorActions(),
    val onCompleteSet: () -> Unit = {},
    val onRemoveActiveSet: () -> Unit = {},
    val onSelectSet: (setId: String) -> Unit = {},
    val onAddSet: () -> Unit = {},
    val rest: RestTimerActions = RestTimerActions(),
    val onPreviousExercise: () -> Unit = {},
    val onNextExercise: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** Stateless Active workout screen. */
@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    actions: ActiveWorkoutActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showEndDialog by rememberSaveable { mutableStateOf(false) }
    val success = uiState as? ActiveWorkoutUiState.Success
    // Finishing asks first only when something would be lost or there is nothing to save.
    val requestFinish = {
        if (success != null && (success.openSetCount > 0 || success.completedSetCount == 0)) {
            showEndDialog = true
        } else {
            actions.onFinish()
        }
    }

    KeepScreenOn(success?.isTiming == true)

    Scaffold(
        modifier = modifier,
        topBar = {
            if (success != null) {
                WorkoutTopBar(
                    workoutName = success.workoutName,
                    startedAt = success.startedAt,
                    exerciseIndex = success.exerciseIndex,
                    exerciseCount = success.exerciseCount,
                    isFinishing = success.isFinishing,
                    onMinimize = actions.onMinimize,
                    onFinish = requestFinish,
                )
            }
        },
        bottomBar = {
            if (success != null) WorkoutBottomBar(success, actions, onFinish = requestFinish)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        WorkoutStateContent(uiState, actions, innerPadding)
    }

    if (showEndDialog && success != null) {
        val dismiss = { showEndDialog = false }
        if (success.completedSetCount == 0) {
            DiscardWorkoutDialog(
                onDiscard = {
                    dismiss()
                    actions.onDiscard()
                },
                onDismiss = dismiss,
            )
        } else {
            FinishWorkoutDialog(
                openSetCount = success.openSetCount,
                onFinish = {
                    dismiss()
                    actions.onFinish()
                },
                onDismiss = dismiss,
            )
        }
    }
}

/** The rest countdown, while there is one, above the exercise navigation. */
@Composable
private fun WorkoutBottomBar(
    state: ActiveWorkoutUiState.Success,
    actions: ActiveWorkoutActions,
    onFinish: () -> Unit,
) {
    Column {
        // Keeps the last rest while it slides away, since the state drops it at once.
        var shownRest by remember { mutableStateOf(state.rest) }
        if (state.rest != null) shownRest = state.rest
        AnimatedVisibility(
            visible = state.rest != null,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            shownRest?.let { RestTimerBar(it, actions.rest) }
        }
        WorkoutFooter(
            hasPrevious = state.previousExerciseName != null,
            nextExerciseName = state.nextExerciseName,
            isFinishing = state.isFinishing,
            onPrevious = actions.onPreviousExercise,
            onNext = actions.onNextExercise,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun WorkoutStateContent(
    uiState: ActiveWorkoutUiState,
    actions: ActiveWorkoutActions,
    innerPadding: PaddingValues,
) {
    StateCrossfade(uiState) { state ->
        when (state) {
            ActiveWorkoutUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            ActiveWorkoutUiState.Error -> ErrorContent(
                message = stringResource(R.string.workout_error_load),
                onRetry = actions.onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            ActiveWorkoutUiState.NotFound -> MessageContent(stringResource(R.string.workout_not_found), Modifier.padding(innerPadding))
            // Moving to the next exercise slides forward; to the previous one, back.
            is ActiveWorkoutUiState.Success -> AnimatedContent(
                targetState = state,
                transitionSpec = { sharedAxisX(forward = targetState.exerciseIndex > initialState.exerciseIndex) },
                contentKey = { it.exerciseIndex },
                label = "exercise",
            ) { WorkoutContent(it, actions, innerPadding) }
        }
    }
}

@Composable
private fun WorkoutContent(
    state: ActiveWorkoutUiState.Success,
    actions: ActiveWorkoutActions,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
        ) {
            item(key = "exercise-${state.exercise.workoutExerciseId}") {
                ExerciseHeader(
                    exercise = state.exercise,
                    onOpenProgress = { actions.onOpenExerciseProgress(state.exercise.exerciseId) },
                    modifier = Modifier.padding(top = spacing.lg, bottom = spacing.md),
                )
            }
            items(state.sets, key = { it.id }) { set ->
                val rowModifier = Modifier
                    .padding(bottom = spacing.sm)
                    .animateItem()
                when (set) {
                    is SetRowUi.Done -> DoneSetRow(set, onEdit = { actions.onSelectSet(set.id) }, modifier = rowModifier)
                    is SetRowUi.Upcoming -> UpcomingSetRow(set, onSelect = { actions.onSelectSet(set.id) }, modifier = rowModifier)
                    is SetRowUi.Active -> ActiveSetCard(
                        set = set,
                        weightRepsActions = actions.weightReps,
                        durationActions = actions.duration,
                        onComplete = actions.onCompleteSet,
                        onRemove = actions.onRemoveActiveSet,
                        modifier = rowModifier,
                    )
                }
            }
            if (state.allSetsDone) {
                item(key = "all-done") { AllSetsDoneBanner(Modifier.padding(bottom = spacing.sm)) }
            }
            item(key = "add-set") {
                TextButton(onClick = actions.onAddSet) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.workout_add_set))
                }
            }
        }
    }
}
