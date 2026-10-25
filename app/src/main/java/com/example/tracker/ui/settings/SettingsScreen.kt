package com.example.tracker.ui.settings

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.BuildConfig
import com.example.tracker.R
import com.example.tracker.domain.export.ExportFormat
import com.example.tracker.domain.health.HealthAvailability
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.domain.workout.RestKind
import com.example.tracker.reminder.canScheduleExactAlarms
import com.example.tracker.ui.common.DestructiveConfirmDialog
import com.example.tracker.ui.common.ExportFormatMenu
import com.example.tracker.ui.common.ObserveAsEvents
import com.example.tracker.ui.common.SectionHeader
import com.example.tracker.ui.common.formatRestTime
import com.example.tracker.ui.common.openAppNotificationSettings
import com.example.tracker.ui.common.openExactAlarmSettings
import com.example.tracker.ui.common.rememberExportLauncher
import com.example.tracker.ui.common.rememberNotificationPermissionRequest
import com.example.tracker.ui.settings.components.DeleteDataDialog
import com.example.tracker.ui.settings.components.HealthConnectActions
import com.example.tracker.ui.settings.components.HealthConnectSection
import com.example.tracker.ui.settings.components.ReminderTimeDialog
import com.example.tracker.ui.settings.components.RestTimeDialog
import com.example.tracker.ui.settings.components.ThemeModeOptions
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.spacing
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
        viewModel.onResume()
        onPauseOrDispose {}
    }

    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectThemeMode = viewModel::onSelectThemeMode,
        onSetDynamicColor = viewModel::onSetDynamicColor,
        onSetReminder = { enabled ->
            viewModel.onSetReminder(enabled)
            if (enabled) ensureNotifications()
        },
        onSetReminderTime = viewModel::onSetReminderTime,
        onSetRestBetweenSets = viewModel::onSetRestBetweenSets,
        onSetRestBetweenExercises = viewModel::onSetRestBetweenExercises,
        exactAlarmsAllowed = exactAlarmsAllowed,
        onAllowExactAlarms = context::openExactAlarmSettings,
        healthActions = HealthConnectActions(
            permissions = viewModel.healthPermissions,
            onPermissionsResult = viewModel::onHealthPermissionsResult,
            onDisable = viewModel::onDisableHealthSync,
            onSyncNow = viewModel::onSyncHealthNow,
        ),
        onDeleteAllData = viewModel::onDeleteAllData,
        exportFileName = viewModel::exportFileName,
        onExportAllData = viewModel::onExportAllData,
        backupFileName = viewModel::backupFileName,
        onBackUp = viewModel::onBackUp,
        onRestore = viewModel::onRestore,
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
    onSetReminder: (Boolean) -> Unit,
    onSetReminderTime: (LocalTime) -> Unit,
    onDeleteAllData: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
    exactAlarmsAllowed: Boolean = true,
    onAllowExactAlarms: () -> Unit = {},
    onSetDynamicColor: (Boolean) -> Unit = {},
    // Wallpaper colors exist from Android 12; a parameter so previews and tests can show both.
    supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    healthActions: HealthConnectActions = HealthConnectActions(),
    onSetRestBetweenSets: (seconds: Int) -> Unit = {},
    onSetRestBetweenExercises: (seconds: Int) -> Unit = {},
    exportFileName: (ExportFormat) -> String = { "" },
    onExportAllData: (ExportFormat, destination: String) -> Unit = { _, _ -> },
    backupFileName: () -> String = { "" },
    onBackUp: (destination: String) -> Unit = {},
    onRestore: (source: String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // The backup file picked to restore, waiting for confirmation.
    var restoreSource by rememberSaveable { mutableStateOf<String?>(null) }
    val pickBackupDestination = rememberDocumentLauncher(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) {
        onBackUp(it)
    }
    val pickRestoreSource = rememberDocumentLauncher(ActivityResultContracts.OpenDocument()) { restoreSource = it }
    var showReminderTimeDialog by rememberSaveable { mutableStateOf(false) }
    var restDialog by rememberSaveable { mutableStateOf<RestKind?>(null) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val spacing = MaterialTheme.spacing

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
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
                if (supportsDynamicColor) {
                    DynamicColorItem(enabled = uiState.dynamicColor, onToggle = onSetDynamicColor)
                }

                SectionHeader(
                    title = stringResource(R.string.settings_notifications),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                )
                ReminderItem(enabled = uiState.reminderEnabled, onToggle = onSetReminder)
                ReminderTimeItem(
                    time = uiState.reminderTime,
                    is24Hour = is24Hour,
                    enabled = uiState.reminderEnabled,
                    onClick = { showReminderTimeDialog = true },
                )
                if (uiState.reminderEnabled && !exactAlarmsAllowed) {
                    ExactAlarmsItem(
                        title = stringResource(R.string.settings_exact_alarms),
                        onClick = onAllowExactAlarms,
                    )
                }

                SectionHeader(
                    title = stringResource(R.string.settings_workout),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                )
                RestTimeItem(
                    title = stringResource(R.string.settings_rest_between_sets),
                    seconds = uiState.restTimes.betweenSetsSeconds,
                    onClick = { restDialog = RestKind.BETWEEN_SETS },
                )
                RestTimeItem(
                    title = stringResource(R.string.settings_rest_between_exercises),
                    seconds = uiState.restTimes.betweenExercisesSeconds,
                    onClick = { restDialog = RestKind.BETWEEN_EXERCISES },
                )
                if (uiState.restTimes.isAnyOn && !exactAlarmsAllowed) {
                    ExactAlarmsItem(
                        title = stringResource(R.string.settings_rest_exact_alarms),
                        onClick = onAllowExactAlarms,
                    )
                }

                if (uiState.health.availability != HealthAvailability.UNSUPPORTED) {
                    SectionHeader(
                        title = stringResource(R.string.settings_health),
                        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                    )
                    HealthConnectSection(uiState.health, healthActions)
                }

                SectionHeader(
                    title = stringResource(R.string.settings_data),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                )
                ExportDataItem(
                    isExporting = uiState.isExportingData,
                    enabled = !uiState.isDataBusy,
                    fileName = exportFileName,
                    onExport = onExportAllData,
                )
                DataActionItem(
                    title = stringResource(R.string.settings_backup),
                    description = stringResource(R.string.settings_backup_description),
                    icon = Icons.Outlined.Backup,
                    inProgress = uiState.isBackingUp,
                    enabled = !uiState.isDataBusy,
                    onClick = { pickBackupDestination(backupFileName()) },
                )
                DataActionItem(
                    title = stringResource(R.string.settings_restore),
                    description = stringResource(R.string.settings_restore_description),
                    icon = Icons.Outlined.Restore,
                    inProgress = uiState.isRestoring,
                    enabled = !uiState.isDataBusy,
                    onClick = { pickRestoreSource(BACKUP_PICKER_TYPES) },
                )
                DeleteDataItem(
                    isDeleting = uiState.isDeletingData,
                    enabled = !uiState.isDataBusy,
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
            initialTime = uiState.reminderTime,
            is24Hour = is24Hour,
            onConfirm = { time ->
                showReminderTimeDialog = false
                onSetReminderTime(time)
            },
            onDismiss = { showReminderTimeDialog = false },
        )
    }

    restDialog?.let { kind ->
        val isSets = kind == RestKind.BETWEEN_SETS
        RestTimeDialog(
            title = stringResource(
                if (isSets) R.string.settings_rest_between_sets else R.string.settings_rest_between_exercises,
            ),
            selectedSeconds = uiState.restTimes.secondsFor(kind),
            onSelect = { seconds ->
                restDialog = null
                if (isSets) onSetRestBetweenSets(seconds) else onSetRestBetweenExercises(seconds)
            },
            onDismiss = { restDialog = null },
        )
    }

    restoreSource?.let { source ->
        DestructiveConfirmDialog(
            title = stringResource(R.string.settings_restore_title),
            body = stringResource(R.string.settings_restore_body),
            confirmLabel = stringResource(R.string.settings_restore_confirm),
            onConfirm = {
                restoreSource = null
                onRestore(source)
            },
            onDismiss = { restoreSource = null },
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

/** "Use wallpaper colors" instead of the blue brand theme. */
@Composable
private fun DynamicColorItem(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
        supportingContent = { Text(stringResource(R.string.settings_dynamic_color_description)) },
        leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle),
    )
}

@Composable
private fun ReminderItem(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_reminder)) },
        supportingContent = { Text(stringResource(R.string.settings_reminder_description)) },
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

/** A rest time and its current value ("1 min 30 sec" or "Off"); tapping changes it. */
@Composable
private fun RestTimeItem(title: String, seconds: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(formatRestTime(seconds)) },
        leadingContent = {
            Icon(if (seconds > 0) Icons.Outlined.Timer else Icons.Outlined.TimerOff, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private const val BACKUP_MIME_TYPE = "application/zip"

/** File managers label zip files differently, so the restore picker accepts each. */
private val BACKUP_PICKER_TYPES = arrayOf(BACKUP_MIME_TYPE, "application/x-zip-compressed", "application/octet-stream")

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/** Asks for "Alarms & reminders" access, so [title]'s alert isn't delayed. */
@Composable
private fun ExactAlarmsItem(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(stringResource(R.string.settings_exact_alarms_description)) },
        leadingContent = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * Returns a function that opens a system file picker with [contract]; [onPicked] gets the
 * chosen file's URI. Cancelling, or a device without a file picker, does nothing.
 */
@Composable
private fun <I> rememberDocumentLauncher(
    contract: ActivityResultContract<I, Uri?>,
    onPicked: (String) -> Unit,
): (I) -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(contract) { uri -> if (uri != null) currentOnPicked(uri.toString()) }
    return { input ->
        try {
            launcher.launch(input)
        } catch (_: ActivityNotFoundException) {
            // No file picker on this device.
        }
    }
}

/** A whole-data action such as backing up; shows progress instead while [inProgress]. */
@Composable
private fun DataActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    inProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = if (inProgress) {
            { CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.spacing.xl)) }
        } else {
            null
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

/** "Export all data": tapping offers PDF or Excel, then asks where to save. */
@Composable
private fun ExportDataItem(
    isExporting: Boolean,
    enabled: Boolean,
    fileName: (ExportFormat) -> String,
    onExport: (ExportFormat, String) -> Unit,
) {
    var showFormats by remember { mutableStateOf(false) }
    val launch = rememberExportLauncher(fileName, onExport)
    Box {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_export_all)) },
            supportingContent = { Text(stringResource(R.string.settings_export_all_description)) },
            leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) },
            trailingContent = if (isExporting) {
                { CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.spacing.xl)) }
            } else {
                null
            },
            modifier = Modifier.clickable(enabled = enabled) { showFormats = true },
        )
        ExportFormatMenu(expanded = showFormats, onDismiss = { showFormats = false }, onSelect = launch)
    }
}

@Composable
private fun DeleteDataItem(isDeleting: Boolean, enabled: Boolean, onClick: () -> Unit) {
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
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
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
