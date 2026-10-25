package com.example.tracker.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.ui.common.AnimatedFab
import com.example.tracker.ui.common.DestructiveConfirmDialog
import com.example.tracker.ui.common.EmptyState
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.ReorderableItem
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.longPressDraggable
import com.example.tracker.ui.common.moveByKey
import com.example.tracker.ui.common.rememberNotificationPermissionRequest
import com.example.tracker.ui.common.rememberReorderableListState
import com.example.tracker.ui.photos.components.ProgressPhotosCard
import com.example.tracker.ui.stats.components.CustomStatCard
import com.example.tracker.ui.stats.components.StatCardMenu
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.Motion
import com.example.tracker.ui.theme.spacing

/** Space below the list so the last card can scroll clear of the floating button. */
private val FabClearance = 88.dp

/** Stats tab destination: connects [StatsViewModel] to [StatsScreen]. */
@Composable
fun StatsRoute(
    onOpenPhotos: () -> Unit,
    onTakePhotos: () -> Unit,
    onNewStat: (StatTemplate?) -> Unit,
    onOpenStat: (statId: String, logToday: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onEditStat: (statId: String) -> Unit = {},
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is StatsEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
    }
    val latestCheckIn by viewModel.latestCheckIn.collectAsStateWithLifecycle()

    // The daily reminder needs notification permission; ask once, here, where the reason is clear.
    val shouldRequestPermission by viewModel.shouldRequestNotificationPermission.collectAsStateWithLifecycle()
    val requestNotifications = rememberNotificationPermissionRequest { viewModel.onNotificationPermissionRequested() }
    LaunchedEffect(shouldRequestPermission) {
        if (shouldRequestPermission) requestNotifications()
    }

    StatsScreen(
        uiState = uiState,
        actions = StatsActions(
            onNewStat = onNewStat,
            onOpenStat = onOpenStat,
            onEditStat = onEditStat,
            onDeleteStat = viewModel::onDeleteStat,
            onReorderStats = viewModel::onReorderStats,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        photosCard = { cardModifier ->
            ProgressPhotosCard(latest = latestCheckIn, onOpen = onOpenPhotos, onTakePhotos = onTakePhotos, modifier = cardModifier)
        },
    )
}

/** What the user can do on the Stats tab. */
data class StatsActions(
    /** Null starts from scratch; the New stat screen offers suggestions. */
    val onNewStat: (StatTemplate?) -> Unit = {},
    /** `logToday` opens today's entry right away. */
    val onOpenStat: (statId: String, logToday: Boolean) -> Unit = { _, _ -> },
    val onEditStat: (statId: String) -> Unit = {},
    /** Called once the user has confirmed. */
    val onDeleteStat: (statId: String) -> Unit = {},
    /** Every stat's id, in the new order. */
    val onReorderStats: (statIds: List<String>) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * Stateless Stats tab: [photosCard], then the user's stats, which a long press lifts to drag
 * into a new order (or, released in place, offers its menu). The large title collapses into
 * the top bar as the list scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    uiState: StatsUiState,
    actions: StatsActions,
    modifier: Modifier = Modifier,
    photosCard: @Composable (Modifier) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.stats_title)) }, scrollBehavior = scrollBehavior)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedFab(visible = uiState is StatsUiState.Success) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.stats_new)) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    onClick = { actions.onNewStat(null) },
                )
            }
        },
    ) { innerPadding ->
        StateCrossfade(uiState) { state ->
            when (state) {
                StatsUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
                StatsUiState.Error -> ErrorContent(
                    message = stringResource(R.string.stats_error_load),
                    onRetry = actions.onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                is StatsUiState.Success -> StatsContent(state, actions, photosCard, innerPadding)
            }
        }
    }
}

@Composable
private fun StatsContent(
    state: StatsUiState.Success,
    actions: StatsActions,
    photosCard: @Composable (Modifier) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    // The stat waiting for the user to confirm its deletion.
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    // The stat whose menu is open.
    var menuId by rememberSaveable { mutableStateOf<String?>(null) }
    // The order shown while dragging; saved on drop, then replaced by the stored order.
    var order by remember { mutableStateOf(state.stats) }
    var storedOrder by remember { mutableStateOf(state.stats) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableListState(
        listState = listState,
        canMove = { key -> order.any { it.stat.id == key } },
        onMove = { from, to -> order = order.moveByKey(from, to) { it.stat.id } },
        onDrop = {
            // Stats added or removed during the drag make the order incomplete; keep the stored one.
            val ids = order.map { it.stat.id }
            val storedIds = state.stats.map { it.stat.id }
            if (ids != storedIds && ids.toSet() == storedIds.toSet()) actions.onReorderStats(ids)
        },
    )
    // Show the stored order when it changes, but never mid-drag, which would drop the drag.
    if (state.stats != storedOrder && reorderState.draggingKey == null) {
        storedOrder = state.stats
        order = state.stats
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = spacing.lg, vertical = spacing.sm)
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
            item(key = "photos") { photosCard(cardModifier) }
            item(key = "mine") {
                StatsHeader(
                    canReorder = order.size > 1,
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.xs),
                )
            }
            if (order.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.QueryStats,
                        title = stringResource(R.string.stats_empty_title),
                        body = stringResource(R.string.stats_empty_body),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            itemsIndexed(order, key = { _, card -> card.stat.id }) { index, card ->
                val id = card.stat.id
                ReorderableItem(reorderState, key = id) { isDragging ->
                    CustomStatCard(
                        card = card,
                        today = state.today,
                        // A long press released in place ends here, still "dragging": open the menu.
                        onClick = { if (reorderState.draggingKey == id) menuId = id else actions.onOpenStat(id, false) },
                        onLog = { actions.onOpenStat(id, true) },
                        menu = StatCardMenu(
                            expanded = menuId == id,
                            onDismiss = { menuId = null },
                            onEdit = { actions.onEditStat(id) },
                            onDelete = { deletingId = id },
                            onMoveUp = if (index > 0) {
                                { actions.onReorderStats(order.map { it.stat.id }.moved(index, index - 1)) }
                            } else {
                                null
                            },
                            onMoveDown = if (index < order.lastIndex) {
                                { actions.onReorderStats(order.map { it.stat.id }.moved(index, index + 1)) }
                            } else {
                                null
                            },
                        ),
                        modifier = cardModifier
                            .lifted(isDragging)
                            .longPressDraggable(reorderState, id),
                    )
                }
            }
        }
    }

    // A stat deleted elsewhere meanwhile just closes the dialog.
    val deleting = state.stats.firstOrNull { it.stat.id == deletingId }?.stat
    if (deleting != null) {
        DestructiveConfirmDialog(
            title = stringResource(R.string.stat_delete_title, deleting.name),
            body = stringResource(R.string.stat_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deletingId = null
                actions.onDeleteStat(deleting.id)
            },
            onDismiss = { deletingId = null },
        )
    }
}

/** "My stats", with a hint on reordering once there is something to reorder. */
@Composable
private fun StatsHeader(canReorder: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.stats_mine),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (canReorder) {
            Text(
                text = stringResource(R.string.stats_reorder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Raises a dragged card above the others, easing in and out so the lift reads as one motion. */
@Composable
private fun Modifier.lifted(isLifted: Boolean): Modifier {
    val lift by animateFloatAsState(if (isLifted) 1f else 0f, tween(Motion.DurationShort), label = "lift")
    val cardShape = CardDefaults.shape
    return graphicsLayer {
        val scale = 1f + LiftScale * lift
        scaleX = scale
        scaleY = scale
        shadowElevation = LiftElevation.toPx() * lift
        shape = cardShape
        clip = false
    }
}

private fun List<String>.moved(from: Int, to: Int): List<String> = toMutableList().apply { add(to, removeAt(from)) }

private const val LiftScale = 0.03f
private val LiftElevation = 8.dp
