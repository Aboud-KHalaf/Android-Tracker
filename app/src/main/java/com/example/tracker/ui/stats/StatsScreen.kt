package com.example.tracker.ui.stats

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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.tracker.ui.common.AnimatedFab
import com.example.tracker.ui.common.DestructiveConfirmDialog
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.ScreenTitle
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.StateCrossfade
import com.example.tracker.ui.common.rememberNotificationPermissionRequest
import com.example.tracker.ui.photos.components.ProgressPhotosCard
import com.example.tracker.ui.stats.components.CustomStatCard
import com.example.tracker.ui.stats.components.StatSuggestionsCard
import com.example.tracker.ui.theme.Dimens
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
    /** Null starts from scratch. */
    val onNewStat: (StatTemplate?) -> Unit = {},
    /** `logToday` opens today's entry right away. */
    val onOpenStat: (statId: String, logToday: Boolean) -> Unit = { _, _ -> },
    val onEditStat: (statId: String) -> Unit = {},
    /** Called once the user has confirmed. */
    val onDeleteStat: (statId: String) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** Stateless Stats tab. [photosCard] goes above the stats. */
@Composable
fun StatsScreen(
    uiState: StatsUiState,
    actions: StatsActions,
    modifier: Modifier = Modifier,
    photosCard: @Composable (Modifier) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
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
                    text = stringResource(R.string.stats_title),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
                )
            }
            item(key = "photos") { photosCard(cardModifier) }
            item(key = "mine") {
                SectionHeader(
                    title = stringResource(R.string.stats_mine),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.sm, top = spacing.md),
                )
            }
            items(state.stats, key = { it.stat.id }) { card ->
                CustomStatCard(
                    card = card,
                    today = state.today,
                    onClick = { actions.onOpenStat(card.stat.id, false) },
                    onLog = { actions.onOpenStat(card.stat.id, true) },
                    onEdit = { actions.onEditStat(card.stat.id) },
                    onDelete = { deletingId = card.stat.id },
                    modifier = cardModifier.animateItem(),
                )
            }
            item(key = "suggestions") {
                StatSuggestionsCard(
                    existingNames = state.stats.mapTo(mutableSetOf()) { it.stat.name },
                    isFirstStat = state.stats.isEmpty(),
                    onPick = actions.onNewStat,
                    modifier = cardModifier.animateItem(),
                )
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
