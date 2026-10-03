package com.example.tracker.ui.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MonthHeader
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.PeriodSelector
import com.example.tracker.ui.common.ScreenTitle
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.weight.components.CurrentWeightCard
import com.example.tracker.ui.weight.components.WeightChartCard
import com.example.tracker.ui.weight.components.WeightEditorDialog
import com.example.tracker.ui.weight.components.WeightRow
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Space below the list so the last row can scroll clear of the floating button. */
private val FabClearance = 88.dp

/** Weight destination: connects [WeightViewModel] to [WeightScreen]. */
@Composable
fun WeightRoute(
    modifier: Modifier = Modifier,
    viewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is WeightEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            // Launched so a second deletion replaces this snackbar instead of waiting for it.
            is WeightEvent.EntryDeleted -> scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.weight_deleted),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(event.entry)
            }
        }
    }

    WeightScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenEntry = viewModel::onOpenEditor,
        onSelectPeriod = viewModel::onSelectPeriod,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )

    val state = uiState
    editor?.let { editorState ->
        WeightEditorDialog(
            state = editorState,
            today = (state as? WeightUiState.Success)?.today ?: editorState.date,
            onDateChange = viewModel::onEditorDateChange,
            onWeightChange = viewModel::onEditorWeightChange,
            onSave = viewModel::onSaveEntry,
            onDelete = viewModel::onDeleteEntry,
            onDismiss = viewModel::onDismissEditor,
        )
    }
}

/** Stateless Weight screen. [onOpenEntry] with null opens today. */
@Composable
fun WeightScreen(
    uiState: WeightUiState,
    onOpenEntry: (LocalDate?) -> Unit,
    onSelectPeriod: (DatePeriod) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is WeightUiState.Success) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.weight_log)) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    onClick = { onOpenEntry(null) },
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            WeightUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            WeightUiState.Error -> ErrorContent(
                message = stringResource(R.string.weight_error_load),
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is WeightUiState.Success -> WeightContent(uiState, onOpenEntry, onSelectPeriod, innerPadding)
        }
    }
}

@Composable
private fun WeightContent(
    state: WeightUiState.Success,
    onOpenEntry: (LocalDate?) -> Unit,
    onSelectPeriod: (DatePeriod) -> Unit,
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
                ScreenTitle(
                    text = stringResource(R.string.weight_title),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
                )
            }
            item(key = "current") {
                CurrentWeightCard(
                    latest = state.latest,
                    today = state.today,
                    onLogToday = { onOpenEntry(state.today) },
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
            if (state.chart != null && state.summary != null) {
                item(key = "chart") { WeightChartCard(state.chart, state.summary, cardModifier) }
            } else {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.MonitorWeight,
                        title = stringResource(R.string.weight_empty_title),
                        body = stringResource(R.string.weight_empty_body),
                    )
                }
            }
            state.months.forEach { group ->
                item(key = "month-${group.month}") {
                    val count = group.entries.size
                    MonthHeader(group.month, pluralStringResource(R.plurals.weight_entry_count, count, count))
                }
                items(group.entries, key = { it.entry.date.toEpochDay() }) { row ->
                    WeightRow(row, onClick = { onOpenEntry(row.entry.date) })
                }
            }
        }
    }
}
