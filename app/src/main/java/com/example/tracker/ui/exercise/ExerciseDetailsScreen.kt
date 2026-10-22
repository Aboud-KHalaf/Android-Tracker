package com.example.tracker.ui.exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.MuscleGroup
import com.example.tracker.ui.exercise.components.MuscleGroupChip
import com.example.tracker.domain.progress.ProgressMetric
import com.example.tracker.domain.progress.TimeRange
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.ExportButton
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.NameInputDialog
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.chart.LineChart
import com.example.tracker.ui.common.label
import com.example.tracker.ui.exercise.components.ExerciseDetailsTopBar
import com.example.tracker.ui.exercise.components.MetricSelector
import com.example.tracker.ui.exercise.components.NoSessions
import com.example.tracker.ui.exercise.components.RangeChips
import com.example.tracker.ui.exercise.components.SessionRow
import com.example.tracker.ui.exercise.components.StatCards
import com.example.tracker.ui.exercise.components.TrendHeadline
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch

/** Exercise details destination: connects [ExerciseDetailsViewModel] to [ExerciseDetailsScreen]. */
@Composable
fun ExerciseDetailsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailsViewModel = viewModel(factory = ExerciseDetailsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ExerciseDetailsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    ExerciseDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = ExerciseDetailsActions(
            onBack = onBack,
            onSelectMetric = viewModel::onSelectMetric,
            onSelectRange = viewModel::onSelectRange,
            onRename = viewModel::onRenameExercise,
            onSetMuscleGroup = viewModel::onSetMuscleGroup,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
        exportButton = {
            ExportButton(isExporting = isExporting, fileName = viewModel::exportFileName, onExport = viewModel::onExport)
        },
    )
}

/** User actions on the Exercise details screen. */
data class ExerciseDetailsActions(
    val onBack: () -> Unit = {},
    val onSelectMetric: (ProgressMetric) -> Unit = {},
    val onSelectRange: (TimeRange) -> Unit = {},
    val onRename: (name: String) -> Unit = {},
    val onSetMuscleGroup: (MuscleGroup?) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** The screen's two tabs. Which one is shown is UI-only state. */
enum class ExerciseTab { PROGRESS, HISTORY }

/** Stateless Exercise details screen. [exportButton] shows in the top bar once the exercise has loaded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailsScreen(
    uiState: ExerciseDetailsUiState,
    actions: ExerciseDetailsActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initialTab: ExerciseTab = ExerciseTab.PROGRESS,
    exportButton: @Composable () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    val success = uiState as? ExerciseDetailsUiState.Success

    Scaffold(
        modifier = modifier,
        topBar = {
            ExerciseDetailsHeader(
                success = success,
                tab = tab,
                onSelectTab = { tab = it },
                onBack = actions.onBack,
                onRename = { showRename = true },
                exportButton = exportButton,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                ExerciseDetailsUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                ExerciseDetailsUiState.Error -> ErrorContent(
                    message = stringResource(R.string.exercise_error_load),
                    onRetry = actions.onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                ExerciseDetailsUiState.NotFound -> MessageContent(
                    message = stringResource(R.string.exercise_not_found),
                    modifier = Modifier.padding(innerPadding),
                )

                is ExerciseDetailsUiState.Success -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = Dimens.maxContentWidth)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xl),
                    ) {
                        item(key = "muscle-group") {
                            MuscleGroupChip(
                                selected = state.muscleGroup,
                                onSelect = actions.onSetMuscleGroup,
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg),
                            )
                        }
                        when {
                            state.sessions.isEmpty() -> item(key = "empty") { NoSessions() }
                            tab == ExerciseTab.PROGRESS -> progressTab(state, actions, onSeeAll = { tab = ExerciseTab.HISTORY })
                            else -> items(state.sessions, key = { it.workoutId }) { SessionRow(it) }
                        }
                    }
                }
            }
        }
    }

    if (showRename && success != null) {
        NameInputDialog(
            title = stringResource(R.string.exercise_rename),
            label = stringResource(R.string.exercise_name_label),
            confirmLabel = stringResource(R.string.plan_rename_confirm),
            initialName = success.name,
            onConfirm = { name ->
                showRename = false
                actions.onRename(name)
            },
            onDismiss = { showRename = false },
        )
    }
}

/** The app bar, with the Progress and History tabs once the exercise has loaded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailsHeader(
    success: ExerciseDetailsUiState.Success?,
    tab: ExerciseTab,
    onSelectTab: (ExerciseTab) -> Unit,
    onBack: () -> Unit,
    onRename: () -> Unit,
    exportButton: @Composable () -> Unit,
) {
    Column {
        ExerciseDetailsTopBar(
            title = success?.name.orEmpty(),
            subtitle = success?.type?.label(),
            onBack = onBack,
            onRename = if (success != null) onRename else null,
            exportButton = { if (success != null) exportButton() },
        )
        if (success != null) {
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                ExerciseTab.entries.forEach { option ->
                    Tab(
                        selected = tab == option,
                        onClick = { onSelectTab(option) },
                        text = { Text(stringResource(option.labelRes)) },
                    )
                }
            }
        }
    }
}

private val ExerciseTab.labelRes: Int
    get() = when (this) {
        ExerciseTab.PROGRESS -> R.string.exercise_tab_progress
        ExerciseTab.HISTORY -> R.string.exercise_tab_history
    }

/** The trend and chart for the chosen metric, or a note that the range has no sessions. */
@Composable
private fun ProgressSection(state: ExerciseDetailsUiState.Success, modifier: Modifier = Modifier) {
    val progress = state.progress
    if (progress != null) {
        Column(modifier = modifier) {
            TrendHeadline(progress, state.metric)
            LineChart(
                chart = progress.chart,
                label = state.metric.label(),
                valueText = { state.metric.valueText(it) },
                axisText = { state.metric.axisText(it) },
                modifier = Modifier.padding(top = MaterialTheme.spacing.lg),
            )
        }
    } else {
        Text(
            text = stringResource(R.string.exercise_no_sessions_in_range),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

private fun LazyListScope.progressTab(
    state: ExerciseDetailsUiState.Success,
    actions: ExerciseDetailsActions,
    onSeeAll: () -> Unit,
) {
    if (state.metrics.size > 1) {
        item(key = "metric") {
            MetricSelector(
                metrics = state.metrics,
                selected = state.metric,
                onSelect = actions.onSelectMetric,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacing.lg)
                    .padding(top = MaterialTheme.spacing.lg),
            )
        }
    }
    item(key = "progress") {
        ProgressSection(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.lg)
                .padding(top = MaterialTheme.spacing.xl),
        )
    }
    item(key = "range") {
        RangeChips(
            selected = state.range,
            onSelect = actions.onSelectRange,
            modifier = Modifier.padding(start = MaterialTheme.spacing.lg, end = MaterialTheme.spacing.lg, top = MaterialTheme.spacing.lg),
        )
    }
    val best = state.personalBest
    val firstSession = state.firstSessionOn
    if (best != null && firstSession != null) {
        item(key = "stats") {
            StatCards(
                personalBest = best,
                sessionCount = state.sessionCount,
                firstSessionOn = firstSession,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacing.lg)
                    .padding(top = MaterialTheme.spacing.xl),
            )
        }
    }
    item(key = "recent-header") {
        SectionHeader(
            title = stringResource(R.string.exercise_recent_sessions),
            actionLabel = stringResource(R.string.home_see_all),
            onAction = onSeeAll,
            modifier = Modifier.padding(start = MaterialTheme.spacing.lg, end = MaterialTheme.spacing.sm, top = MaterialTheme.spacing.xl),
        )
    }
    items(state.recentSessions, key = { "recent-${it.workoutId}" }) { SessionRow(it, Modifier.animateItem()) }
}
