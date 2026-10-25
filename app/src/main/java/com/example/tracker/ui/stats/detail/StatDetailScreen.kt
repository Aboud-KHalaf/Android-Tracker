package com.example.tracker.ui.stats.detail

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.DatePeriod
import com.example.tracker.ui.common.AnimatedFab
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.ExportButton
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MonthHeader
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.PeriodSelector
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.stats.detail.components.CurrentStatCard
import com.example.tracker.ui.stats.detail.components.StatChartCard
import com.example.tracker.ui.stats.detail.components.StatEntryDialog
import com.example.tracker.ui.stats.detail.components.StatRow
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Space below the list so the last row can scroll clear of the floating button. */
private val FabClearance = 88.dp

/** Custom stat destination: connects [StatDetailViewModel] to [StatDetailScreen]. */
@Composable
fun StatDetailRoute(
    onBack: () -> Unit,
    onEdit: (statId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatDetailViewModel = viewModel(factory = StatDetailViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is StatDetailEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            // Launched so a second deletion replaces this snackbar instead of waiting for it.
            is StatDetailEvent.EntryDeleted -> scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.stat_entry_deleted),
                    actionLabel = context.getString(R.string.action_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(event.entry)
            }
        }
    }

    val state = uiState
    // Deleted from its editor while this screen was underneath.
    LaunchedEffect(state is StatDetailUiState.Missing) {
        if (state is StatDetailUiState.Missing) onBack()
    }

    StatDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = { (state as? StatDetailUiState.Success)?.let { onEdit(it.stat.id) } },
        onOpenEntry = viewModel::onOpenEditor,
        onSelectPeriod = viewModel::onSelectPeriod,
        onRetry = viewModel::onRetry,
        modifier = modifier,
        exportButton = {
            ExportButton(isExporting = isExporting, fileName = viewModel::exportFileName, onExport = viewModel::onExport)
        },
    )

    val success = state as? StatDetailUiState.Success
    val editorState = editor
    if (success != null && editorState != null) {
        StatEntryDialog(
            stat = success.stat,
            state = editorState,
            today = success.today,
            onDateChange = viewModel::onEditorDateChange,
            onValueChange = viewModel::onEditorValueChange,
            onSave = viewModel::onSaveEntry,
            onDelete = viewModel::onDeleteEntry,
            onDismiss = viewModel::onDismissEditor,
        )
    }
}

/** Stateless custom stat screen. [onOpenEntry] with null opens today; [exportButton] sits in the top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDetailScreen(
    uiState: StatDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenEntry: (LocalDate?) -> Unit,
    onSelectPeriod: (DatePeriod) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    exportButton: @Composable () -> Unit = {},
) {
    val success = uiState as? StatDetailUiState.Success
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(success?.stat?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (success != null) {
                        exportButton()
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.stat_edit))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedFab(visible = success != null) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.stat_log)) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    onClick = { onOpenEntry(null) },
                )
            }
        },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                StatDetailUiState.Loading, StatDetailUiState.Missing -> LoadingContent(Modifier.padding(innerPadding))
                StatDetailUiState.Error -> ErrorContent(
                    message = stringResource(R.string.stat_error_load),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                is StatDetailUiState.Success -> StatDetailContent(state, onOpenEntry, onSelectPeriod, innerPadding)
            }
        }
    }
}

@Composable
private fun StatDetailContent(
    state: StatDetailUiState.Success,
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
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + FabClearance,
            ),
        ) {
            item(key = "current") {
                CurrentStatCard(
                    stat = state.stat,
                    latest = state.latest,
                    progress = state.progress,
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
                item(key = "chart") { StatChartCard(state.stat, state.chart, state.summary, cardModifier) }
            } else {
                item(key = "empty") {
                    EmptyState(
                        icon = state.stat.icon.imageVector,
                        title = stringResource(R.string.stat_empty_title),
                        body = stringResource(R.string.stat_empty_body),
                    )
                }
            }
            state.months.forEach { group ->
                stickyHeader(key = "month-${group.month}") {
                    val count = group.entries.size
                    MonthHeader(
                        month = group.month,
                        countText = pluralStringResource(R.plurals.stat_entry_count, count, count),
                    )
                }
                items(group.entries, key = { it.entry.date.toEpochDay() }) { row ->
                    StatRow(state.stat, row, onClick = { onOpenEntry(row.entry.date) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}
