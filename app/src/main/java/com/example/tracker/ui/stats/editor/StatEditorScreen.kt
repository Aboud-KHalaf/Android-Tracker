package com.example.tracker.ui.stats.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatGoal
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.ui.common.DestructiveConfirmDialog
import com.example.tracker.ui.common.ErrorContent
import com.example.tracker.ui.common.IconAvatar
import com.example.tracker.ui.common.LoadingContent
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.stats.formatStatNumber
import com.example.tracker.ui.stats.imageVector
import com.example.tracker.ui.stats.editor.components.StatColorPicker
import com.example.tracker.ui.stats.editor.components.StatIconPicker
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import com.example.tracker.ui.theme.statColors
import kotlinx.coroutines.launch

/** Stat editor destination: connects [StatEditorViewModel] to [StatEditorScreen]. */
@Composable
fun StatEditorRoute(
    onClose: () -> Unit,
    onCreated: (statId: String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatEditorViewModel = viewModel(factory = StatEditorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is StatEditorEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }

            is StatEditorEvent.Created -> onCreated(event.statId)
            StatEditorEvent.Saved -> onClose()
            StatEditorEvent.Deleted -> onDeleted()
        }
    }

    StatEditorScreen(
        uiState = uiState,
        actions = StatEditorActions(
            onClose = onClose,
            onSave = viewModel::onSave,
            onDelete = viewModel::onDelete,
            onRetry = viewModel::onRetry,
            onNameChange = viewModel::onNameChange,
            onUnitChange = viewModel::onUnitChange,
            onDecimalsChange = viewModel::onDecimalsChange,
            onTargetChange = viewModel::onTargetChange,
            onGoalChange = viewModel::onGoalChange,
            onIconChange = viewModel::onIconChange,
            onColorChange = viewModel::onColorChange,
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** What the user can do in the stat editor. */
data class StatEditorActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    /** Called once the user has confirmed. */
    val onDelete: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onUnitChange: (String) -> Unit = {},
    val onDecimalsChange: (Int) -> Unit = {},
    val onTargetChange: (String) -> Unit = {},
    val onGoalChange: (StatGoal) -> Unit = {},
    val onIconChange: (StatIcon) -> Unit = {},
    val onColorChange: (StatColor) -> Unit = {},
)

/** Stateless stat editor, full screen with Save in the top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatEditorScreen(
    uiState: StatEditorUiState,
    actions: StatEditorActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val editing = uiState as? StatEditorUiState.Editing
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (editing?.isNew == false) R.string.stat_edit else R.string.stats_new)) },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    if (editing != null) {
                        TextButton(onClick = actions.onSave, enabled = !editing.isSaving) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            StatEditorUiState.Loading -> LoadingContent(Modifier.padding(innerPadding))
            StatEditorUiState.Error -> ErrorContent(
                message = stringResource(R.string.stat_error_load),
                onRetry = actions.onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is StatEditorUiState.Editing -> StatForm(uiState, actions, onDelete = { confirmDelete = true }, innerPadding)
        }
    }

    if (confirmDelete) {
        DestructiveConfirmDialog(
            title = stringResource(R.string.stat_delete_title, editing?.form?.name.orEmpty()),
            body = stringResource(R.string.stat_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                actions.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun StatForm(
    state: StatEditorUiState.Editing,
    actions: StatEditorActions,
    onDelete: () -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = MaterialTheme.spacing
    val form = state.form
    val accent = MaterialTheme.statColors[form.color]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            // How the stat will look on the Stats tab.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                IconAvatar(
                    icon = form.icon.imageVector,
                    containerColor = accent.container,
                    contentColor = accent.content,
                    size = Dimens.minTouchTarget,
                )
                Text(
                    text = form.name.ifBlank { stringResource(R.string.stat_name_placeholder) },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (form.name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = actions.onNameChange,
                label = { Text(stringResource(R.string.stat_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.stat_error_name)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.unit,
                onValueChange = actions.onUnitChange,
                label = { Text(stringResource(R.string.stat_unit)) },
                placeholder = { Text(stringResource(R.string.stat_unit_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            DecimalsSelector(form.decimals, actions.onDecimalsChange)
            OutlinedTextField(
                value = form.target,
                onValueChange = actions.onTargetChange,
                label = { Text(stringResource(R.string.stat_target_optional)) },
                suffix = form.unit.takeIf { it.isNotBlank() }?.let { unit -> { Text(unit) } },
                isError = state.targetError,
                supportingText = if (state.targetError) {
                    { Text(stringResource(R.string.stat_error_value)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (form.decimals > 0) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (form.target.isNotBlank()) GoalSelector(form.goal, actions.onGoalChange)
            Section(stringResource(R.string.stat_icon)) {
                StatIconPicker(selected = form.icon, accent = accent, onSelect = actions.onIconChange)
            }
            Section(stringResource(R.string.stat_color)) {
                StatColorPicker(selected = form.color, onSelect = actions.onColorChange)
            }
            if (!state.isNew) {
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.stat_delete))
                }
            }
        }
    }
}

/** 0, 1 or 2 decimal places, each shown as an example value. */
@Composable
private fun DecimalsSelector(selected: Int, onSelect: (Int) -> Unit) {
    val locale = currentLocale()
    Section(stringResource(R.string.stat_decimals)) {
        val options = CustomStat.DECIMALS_RANGE.toList()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, decimals ->
                SegmentedButton(
                    selected = decimals == selected,
                    onClick = { onSelect(decimals) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(formatStatNumber(EXAMPLE_VALUE, decimals, locale)) }
            }
        }
    }
}

private const val EXAMPLE_VALUE = 72.5

/** Whether the target is an amount to reach each day or a change over time, with what that means. */
@Composable
private fun GoalSelector(selected: StatGoal, onSelect: (StatGoal) -> Unit) {
    Section(stringResource(R.string.stat_goal)) {
        val options = listOf(StatGoal.DAILY to R.string.stat_goal_daily, StatGoal.CHANGE to R.string.stat_goal_change)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (goal, labelRes) ->
                SegmentedButton(
                    selected = goal == selected,
                    onClick = { onSelect(goal) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(stringResource(labelRes)) }
            }
        }
        Text(
            text = stringResource(
                when (selected) {
                    StatGoal.DAILY -> R.string.stat_goal_daily_hint
                    StatGoal.CHANGE -> R.string.stat_goal_change_hint
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}
