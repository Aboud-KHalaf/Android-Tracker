package com.example.tracker.ui.settings

import android.text.format.DateFormat
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.R
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.reminder.canScheduleExactAlarms
import com.example.tracker.ui.common.openAppNotificationSettings
import com.example.tracker.ui.common.openExactAlarmSettings
import com.example.tracker.ui.common.rememberNotificationPermissionRequest
import com.example.tracker.ui.settings.components.DeleteDataDialog
import com.example.tracker.ui.settings.components.ReminderTimeDialog
import com.example.tracker.ui.settings.components.ThemeModeOptions
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    // Re-checked on every resume, since the user grants it on a system settings page.
    var exactAlarmsAllowed by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    LifecycleResumeEffect(Unit) {
        exactAlarmsAllowed = canScheduleExactAlarms(context)
        onPauseOrDispose {}
    }

    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectThemeMode = viewModel::onSelectThemeMode,
        onSetNutritionReminder = { enabled ->
            viewModel.onSetNutritionReminder(enabled)
            if (enabled) ensureNotifications()
        },
        onSetNutritionReminderTime = viewModel::onSetNutritionReminderTime,
        showExactAlarmsItem = uiState.nutritionReminderEnabled && !exactAlarmsAllowed,
        onAllowExactAlarms = context::openExactAlarmSettings,
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
    onSetNutritionReminderTime: (LocalTime) -> Unit,
    onDeleteAllData: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
    showExactAlarmsItem: Boolean = false,
    onAllowExactAlarms: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderTimeDialog by rememberSaveable { mutableStateOf(false) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
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
                ReminderTimeItem(
                    time = uiState.nutritionReminderTime,
                    is24Hour = is24Hour,
                    enabled = uiState.nutritionReminderEnabled,
                    onClick = { showReminderTimeDialog = true },
                )
                if (showExactAlarmsItem) ExactAlarmsItem(onClick = onAllowExactAlarms)

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

    if (showReminderTimeDialog) {
        ReminderTimeDialog(
            initialTime = uiState.nutritionReminderTime,
            is24Hour = is24Hour,
            onConfirm = { time ->
                showReminderTimeDialog = false
                onSetNutritionReminderTime(time)
            },
            onDismiss = { showReminderTimeDialog = false },
        )
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

/** Shows the reminder time in the user's 12- or 24-hour format; dimmed while the reminder is off. */
@Composable
private fun ReminderTimeItem(time: LocalTime, is24Hour: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = remember(time, is24Hour, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hma")
        time.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    val colors = if (enabled) {
        ListItemDefaults.colors()
    } else {
        val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
        ListItemDefaults.colors(
            headlineColor = disabled,
            supportingColor = disabled,
            leadingIconColor = disabled,
        )
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
        supportingContent = { Text(formatted) },
        leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
        colors = colors,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

@Composable
private fun ExactAlarmsItem(onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_exact_alarms)) },
        supportingContent = { Text(stringResource(R.string.settings_exact_alarms_description)) },
        leadingContent = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
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
