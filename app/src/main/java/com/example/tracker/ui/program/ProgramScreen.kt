package com.example.tracker.ui.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.DestructiveConfirmDialog
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.GroupedListGap
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.MessageContent
import com.example.tracker.ui.common.NameInputDialog
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.RenameDeleteTopBar
import com.example.tracker.ui.common.ReorderHandle
import com.example.tracker.ui.common.ReorderableItem
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.fullName
import com.example.tracker.ui.common.groupedListItemShape
import com.example.tracker.ui.common.longPressDraggable
import com.example.tracker.ui.common.moveByKey
import com.example.tracker.ui.common.rememberReorderableListState
import com.example.tracker.ui.program.components.ProgramDayRow
import com.example.tracker.ui.program.components.ProgramDayRowActions
import com.example.tracker.ui.program.components.ScheduleDayDialog
import com.example.tracker.ui.program.components.WeekdaySelector
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.time.DayOfWeek

/**
 * Program destination: connects [ProgramViewModel] to [ProgramScreen] and turns its events
 * into navigation and snackbars.
 */
@Composable
fun ProgramRoute(
    onBack: () -> Unit,
    onOpenDay: (planId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramViewModel = viewModel(factory = ProgramViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProgramEvent.ProgramDeleted -> onBack()
            is ProgramEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
    }

    ProgramScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = ProgramActions(
            onBack = onBack,
            onOpenDay = onOpenDay,
            onAddDay = viewModel::onAddDay,
            onToggleWeekday = viewModel::onToggleWeekday,
            onRequestRemoveDay = viewModel::onRequestRemoveDay,
            onConfirmRemoveDay = viewModel::onConfirmRemoveDay,
            onCancelRemoveDay = viewModel::onCancelRemoveDay,
            onScheduleDay = viewModel::onScheduleDay,
            onReorderDays = viewModel::onReorderDays,
            onMakeActive = viewModel::onMakeActive,
            onRenameProgram = viewModel::onRenameProgram,
            onDeleteProgram = viewModel::onDeleteProgram,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

/** User actions on the Program screen. */
data class ProgramActions(
    val onBack: () -> Unit = {},
    val onOpenDay: (planId: String) -> Unit = {},
    val onAddDay: (dayOfWeek: DayOfWeek?, name: String) -> Unit = { _, _ -> },
    val onToggleWeekday: (dayOfWeek: DayOfWeek, defaultName: String) -> Unit = { _, _ -> },
    val onRequestRemoveDay: (planId: String) -> Unit = {},
    val onConfirmRemoveDay: () -> Unit = {},
    val onCancelRemoveDay: () -> Unit = {},
    val onScheduleDay: (planId: String, dayOfWeek: DayOfWeek?) -> Unit = { _, _ -> },
    /** Every day's id, in the new order. */
    val onReorderDays: (dayIds: List<String>) -> Unit = {},
    val onMakeActive: () -> Unit = {},
    val onRenameProgram: (name: String) -> Unit = {},
    val onDeleteProgram: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * Which dialog is open; only one at a time. SCHEDULE_DAY acts on the day in `selectedDayId`.
 * Confirming a day's removal is driven by the ViewModel instead.
 */
private enum class ProgramOverlay { NONE, RENAME, DELETE, ADD_DAY, SCHEDULE_DAY }

/** Stateless Program screen. */
@Composable
fun ProgramScreen(
    uiState: ProgramUiState,
    actions: ProgramActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var overlay by rememberSaveable { mutableStateOf(ProgramOverlay.NONE) }
    var selectedDayId by rememberSaveable { mutableStateOf<String?>(null) }
    val success = uiState as? ProgramUiState.Success
    val locale = currentLocale()

    fun openDayOverlay(dayId: String, kind: ProgramOverlay) {
        selectedDayId = dayId
        overlay = kind
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RenameDeleteTopBar(
                title = success?.name.orEmpty(),
                showActions = success != null,
                optionsDescription = stringResource(R.string.program_options),
                renameLabel = stringResource(R.string.program_rename),
                deleteLabel = stringResource(R.string.program_delete),
                onBack = actions.onBack,
                onRename = { overlay = ProgramOverlay.RENAME },
                onDelete = { overlay = ProgramOverlay.DELETE },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                ProgramUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                ProgramUiState.Error -> ErrorContent(
                    message = stringResource(R.string.program_error_load),
                    onRetry = actions.onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                ProgramUiState.NotFound -> MessageContent(
                    stringResource(R.string.program_not_found),
                    Modifier.padding(innerPadding),
                )

                is ProgramUiState.Success -> ProgramContent(
                    state = state,
                    actions = actions,
                    onToggleWeekday = { weekday -> actions.onToggleWeekday(weekday, weekday.fullName(locale)) },
                    onChangeWeekday = { openDayOverlay(it, ProgramOverlay.SCHEDULE_DAY) },
                    onRemoveDay = actions.onRequestRemoveDay,
                    onAddFlexibleDay = { overlay = ProgramOverlay.ADD_DAY },
                    contentPadding = innerPadding,
                )
            }
        }
    }

    if (success != null) {
        val selectedDay = success.days.firstOrNull { it.id == selectedDayId }
        ProgramOverlays(overlay, success, selectedDay, actions, onDismiss = { overlay = ProgramOverlay.NONE })
        success.removalToConfirm?.let { day ->
            DestructiveConfirmDialog(
                title = stringResource(R.string.program_remove_day_title, day.name),
                body = stringResource(R.string.program_remove_day_body),
                confirmLabel = stringResource(R.string.program_remove_day_confirm),
                onConfirm = actions.onConfirmRemoveDay,
                onDismiss = actions.onCancelRemoveDay,
            )
        }
    }
}

@Composable
private fun ProgramOverlays(
    overlay: ProgramOverlay,
    state: ProgramUiState.Success,
    selectedDay: ProgramDayUi?,
    actions: ProgramActions,
    onDismiss: () -> Unit,
) {
    when (overlay) {
        ProgramOverlay.NONE -> Unit
        ProgramOverlay.RENAME -> NameInputDialog(
            title = stringResource(R.string.program_rename),
            label = stringResource(R.string.program_name_label),
            confirmLabel = stringResource(R.string.plan_rename_confirm),
            initialName = state.name,
            onConfirm = { name ->
                onDismiss()
                actions.onRenameProgram(name)
            },
            onDismiss = onDismiss,
        )

        ProgramOverlay.DELETE -> DestructiveConfirmDialog(
            title = stringResource(R.string.plan_delete_title, state.name),
            body = stringResource(R.string.program_delete_body),
            confirmLabel = stringResource(R.string.plan_delete_confirm),
            onConfirm = {
                onDismiss()
                actions.onDeleteProgram()
            },
            onDismiss = onDismiss,
        )

        ProgramOverlay.ADD_DAY -> NameInputDialog(
            title = stringResource(R.string.program_new_day_title),
            label = stringResource(R.string.plan_name_label),
            confirmLabel = stringResource(R.string.program_new_day_confirm),
            onConfirm = { name ->
                onDismiss()
                actions.onAddDay(null, name)
            },
            onDismiss = onDismiss,
        )

        // The day may disappear while its dialog is open (e.g. deleted elsewhere); then close it.
        ProgramOverlay.SCHEDULE_DAY -> if (selectedDay == null) {
            LaunchedEffect(Unit) { onDismiss() }
        } else {
            ScheduleDayDialog(
                day = selectedDay,
                allDays = state.days,
                onSchedule = { weekday ->
                    onDismiss()
                    actions.onScheduleDay(selectedDay.id, weekday)
                },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ProgramContent(
    state: ProgramUiState.Success,
    actions: ProgramActions,
    onToggleWeekday: (DayOfWeek) -> Unit,
    onChangeWeekday: (dayId: String) -> Unit,
    onRemoveDay: (dayId: String) -> Unit,
    onAddFlexibleDay: () -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    // The order shown while dragging; saved on drop, then replaced by the stored order.
    var order by remember { mutableStateOf(state.days) }
    var storedOrder by remember { mutableStateOf(state.days) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableListState(
        listState = listState,
        canMove = { key -> order.any { it.id == key } },
        onMove = { from, to -> order = order.moveByKey(from, to) { it.id } },
        onDrop = {
            // Days added or removed during the drag make the order incomplete; keep the stored one.
            val ids = order.map { it.id }
            val storedIds = state.days.map { it.id }
            if (ids != storedIds && ids.toSet() == storedIds.toSet()) actions.onReorderDays(ids)
        },
    )
    // Show the stored order when it changes, but never mid-drag, which would drop the drag.
    if (state.days != storedOrder && reorderState.draggingKey == null) {
        storedOrder = state.days
        order = state.days
    }
    fun moveDay(from: Int, to: Int) = actions.onReorderDays(order.map { it.id }.toMutableList().apply { add(to, removeAt(from)) })
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.lg,
                end = spacing.lg,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + spacing.xl,
            ),
        ) {
            item(key = "status") {
                ActiveStatusCard(isActive = state.isActive, onMakeActive = actions.onMakeActive)
            }
            item(key = "schedule") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SectionHeader(
                        title = stringResource(R.string.program_training_days),
                        modifier = Modifier.padding(top = spacing.lg),
                    )
                    WeekdaySelector(selected = state.trainingDays, onToggle = onToggleWeekday)
                    Text(
                        text = stringResource(R.string.program_training_days_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "days-header") {
                Column(modifier = Modifier.padding(top = spacing.lg, bottom = spacing.sm)) {
                    SectionHeader(title = stringResource(R.string.program_days))
                    if (order.size > 1) {
                        Text(
                            text = stringResource(R.string.program_days_reorder_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.days.isEmpty()) {
                item(key = "days-empty") {
                    Text(
                        text = stringResource(R.string.program_no_days),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            itemsIndexed(order, key = { _, day -> day.id }) { index, day ->
                ReorderableItem(reorderState, key = day.id, modifier = Modifier.padding(top = if (index == 0) 0.dp else GroupedListGap)) {
                    ProgramDayRow(
                        day = day,
                        shape = groupedListItemShape(index, order.size),
                        onClick = { actions.onOpenDay(day.id) },
                        actions = ProgramDayRowActions(
                            onChangeWeekday = { onChangeWeekday(day.id) },
                            onRemove = { onRemoveDay(day.id) },
                            onMoveUp = if (index > 0) ({ moveDay(index, index - 1) }) else null,
                            onMoveDown = if (index < order.lastIndex) ({ moveDay(index, index + 1) }) else null,
                        ),
                        dragHandle = {
                            ReorderHandle(
                                state = reorderState,
                                key = day.id,
                                contentDescription = stringResource(R.string.program_day_drag_handle, day.name),
                            )
                        },
                        modifier = Modifier.longPressDraggable(reorderState, day.id),
                    )
                }
            }
            item(key = "add-day") {
                OutlinedButton(
                    onClick = onAddFlexibleDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.lg)
                        .heightIn(min = Dimens.minTouchTarget),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.program_add_flexible_day))
                }
            }
        }
    }
}

/** Says whether Home follows this program, and offers to switch to it when it doesn't. */
@Composable
private fun ActiveStatusCard(isActive: Boolean, onMakeActive: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        },
    ) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            if (isActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(spacing.sm))
                    Text(stringResource(R.string.home_active_program), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.program_active_body), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(R.string.program_inactive_body), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(
                    onClick = onMakeActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.minTouchTarget),
                ) { Text(stringResource(R.string.program_make_active)) }
            }
        }
    }
}
