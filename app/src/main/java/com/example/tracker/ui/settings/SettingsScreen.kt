package com.example.tracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.example.tracker.BuildConfig
import androidx.compose.foundation.selection.toggleable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.openAppNotificationSettings
import com.example.tracker.ui.common.rememberNotificationPermissionRequest
import com.example.tracker.ui.settings.components.DeleteDataDialog
import com.example.tracker.ui.settings.components.ThemeModeOptions
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch

/** Settings destination: connects [SettingsViewModel] to [SettingsScreen]. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
    }

    val scope = rememberCoroutineScope()
    val ensureNotifications = rememberNotificationPermissionRequest { granted ->
        if (!granted) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.settings_notifications_blocked),
                    actionLabel = context.getString(R.string.settings_open_system_settings),
                )
                if (result == SnackbarResult.ActionPerformed) context.openAppNotificationSettings()
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectThemeMode = viewModel::onSelectThemeMode,
        onSetNutritionReminder = { enabled ->
            viewModel.onSetNutritionReminder(enabled)
            if (enabled) ensureNotifications()
        },
        onDeleteAllData = viewModel::onDeleteAllData,
        appVersion = BuildConfig.VERSION_NAME,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Stateless Settings screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSetNutritionReminder: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val spacing = MaterialTheme.spacing

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimens.maxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = spacing.lg),
            ) {
                SectionHeader(
                    title = stringResource(R.string.settings_appearance),
                    modifier = Modifier.padding(horizontal = spacing.lg),
                )
                ThemeModeOptions(selected = uiState.themeMode, onSelect = onSelectThemeMode)

                SectionHeader(
                    title = stringResource(R.string.settings_notifications),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                )
                ReminderItem(enabled = uiState.nutritionReminderEnabled, onToggle = onSetNutritionReminder)

                SectionHeader(
                    title = stringResource(R.string.settings_data),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                )
                DeleteDataItem(
                    isDeleting = uiState.isDeletingData,
                    onClick = { showDeleteDialog = true },
                )

                SectionHeader(
                    title = stringResource(R.string.settings_about),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                )
                VersionItem(appVersion = appVersion)
                BuiltByText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteDataDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteAllData()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun ReminderItem(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_nutrition_reminder)) },
        supportingContent = { Text(stringResource(R.string.settings_nutrition_reminder_description)) },
        leadingContent = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle),
    )
}

@Composable
private fun DeleteDataItem(isDeleting: Boolean, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_delete_data)) },
        supportingContent = { Text(stringResource(R.string.settings_delete_data_description)) },
        leadingContent = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
        trailingContent = if (isDeleting) {
            { CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.spacing.xl)) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(headlineColor = error, leadingIconColor = error),
        modifier = Modifier.clickable(enabled = !isDeleting, onClick = onClick),
    )
}

@Composable
private fun VersionItem(appVersion: String) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_version)) },
        supportingContent = { Text(appVersion) },
        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
    )
}

/** "Built by Aboud", where the name opens the author's portfolio in the browser. */
@Composable
private fun BuiltByText(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.settings_author_name)
    val url = stringResource(R.string.settings_author_url)
    val fullText = stringResource(R.string.settings_built_by, name)
    val nameStart = fullText.indexOf(name)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val text = buildAnnotatedString {
        append(fullText)
        if (nameStart >= 0) {
            addLink(LinkAnnotation.Url(url, linkStyles), nameStart, nameStart + name.length)
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
