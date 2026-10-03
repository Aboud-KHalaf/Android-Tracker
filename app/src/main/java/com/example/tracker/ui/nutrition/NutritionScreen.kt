package com.example.tracker.ui.nutrition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.NutritionTargets
import com.example.tracker.domain.nutrition.NutritionPeriod
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MonthHeader
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.ScreenTitle
import com.example.tracker.ui.nutrition.components.DayEditorDialog
import com.example.tracker.ui.nutrition.components.NutritionChartCard
import com.example.tracker.ui.nutrition.components.NutritionDayRow
import com.example.tracker.ui.nutrition.components.PeriodSelector
import com.example.tracker.ui.nutrition.components.PeriodSummaryCard
import com.example.tracker.ui.nutrition.components.TargetsDialog
import com.example.tracker.ui.nutrition.components.TodayCard
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Space below the list so the last row can scroll clear of the floating button. */
private val FabClearance = 88.dp

/** Nutrition destination: connects [NutritionViewModel] to [NutritionScreen]. */
@Composable
fun NutritionRoute(
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NutritionEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            // Launched so a second deletion replaces this snackbar instead of waiting for it.
            is NutritionEvent.DayDeleted -> scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.nutrition_day_deleted),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(event.day)
            }
        }
    }

    NutritionScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenDay = viewModel::onOpenEditor,
        onSaveTargets = viewModel::onSaveTargets,
        onSelectPeriod = viewModel::onSelectPeriod,
        onSelectMetric = viewModel::onSelectMetric,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )

    val state = uiState
    editor?.let { editorState ->
        DayEditorDialog(
            state = editorState,
            today = (state as? NutritionUiState.Success)?.today ?: editorState.date,
            onDateChange = viewModel::onEditorDateChange,
            onCaloriesChange = viewModel::onEditorCaloriesChange,
            onProteinChange = viewModel::onEditorProteinChange,
            onSave = viewModel::onSaveDay,
            onDelete = viewModel::onDeleteDay,
            onDismiss = viewModel::onDismissEditor,
        )
    }
}

/** Stateless Nutrition screen. [onOpenDay] with null opens today. */
@Composable
fun NutritionScreen(
    uiState: NutritionUiState,
    onOpenDay: (LocalDate?) -> Unit,
    onSaveTargets: (NutritionTargets) -> Unit,
    onSelectPeriod: (NutritionPeriod) -> Unit,
    onSelectMetric: (NutritionMetric) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showTargetsDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is NutritionUiState.Success) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.nutrition_log_day)) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    onClick = { onOpenDay(null) },
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            NutritionUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            NutritionUiState.Error -> ErrorContent(
                message = stringResource(R.string.nutrition_error_load),
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is NutritionUiState.Success -> NutritionContent(
                state = uiState,
                onOpenDay = onOpenDay,
                onEditTargets = { showTargetsDialog = true },
                onSelectPeriod = onSelectPeriod,
                onSelectMetric = onSelectMetric,
                contentPadding = innerPadding,
            )
        }
    }

    if (showTargetsDialog && uiState is NutritionUiState.Success) {
        TargetsDialog(
            targets = uiState.targets,
            onSave = { targets ->
                showTargetsDialog = false
                onSaveTargets(targets)
            },
            onDismiss = { showTargetsDialog = false },
        )
    }
}

@Composable
private fun NutritionContent(
    state: NutritionUiState.Success,
    onOpenDay: (LocalDate?) -> Unit,
    onEditTargets: () -> Unit,
    onSelectPeriod: (NutritionPeriod) -> Unit,
    onSelectMetric: (NutritionMetric) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = spacing.lg, vertical = spacing.sm)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + spacing.xl,
                bottom = contentPadding.calculateBottomPadding() + FabClearance,
            ),
        ) {
            item(key = "title") {
                TitleRow(
                    onEditTargets = onEditTargets,
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.sm, bottom = spacing.md),
                )
            }
            item(key = "today") {
                TodayCard(
                    today = state.today,
                    entry = state.todayEntry,
                    targets = state.targets,
                    onLogToday = { onOpenDay(state.today) },
                    modifier = cardModifier,
                )
            }
            item(key = "period") {
                PeriodSelector(
                    selected = state.period,
                    rangeStart = state.rangeStart,
                    rangeEnd = state.rangeEnd,
                    today = state.today,
                    onSelect = onSelectPeriod,
                    modifier = Modifier.padding(top = spacing.md, bottom = spacing.xs),
                )
            }
            state.chart?.let { chart ->
                item(key = "chart") {
                    NutritionChartCard(chart, state.metric, onSelectMetric, cardModifier)
                }
            }
            if (state.summary == null) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.Restaurant,
                        title = stringResource(R.string.nutrition_empty_title),
                        body = stringResource(R.string.nutrition_empty_body),
                    )
                }
            } else {
                item(key = "summary") {
                    PeriodSummaryCard(state.summary, state.rangeDayCount, cardModifier)
                }
            }
            state.months.forEach { group ->
                item(key = "month-${group.month}") {
                    val count = group.days.size
                    MonthHeader(group.month, pluralStringResource(R.plurals.nutrition_day_count, count, count))
                }
                items(group.days, key = { it.date.toEpochDay() }) { day ->
                    NutritionDayRow(day, onClick = { onOpenDay(day.date) })
                }
            }
        }
    }
}

/** "Nutrition" with a "Targets" action on the right. */
@Composable
private fun TitleRow(onEditTargets: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ScreenTitle(text = stringResource(R.string.nutrition_title), modifier = Modifier.weight(1f))
        TextButton(onClick = onEditTargets, modifier = Modifier.heightIn(min = Dimens.minTouchTarget)) {
            Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.nutrition_targets))
        }
    }
}
