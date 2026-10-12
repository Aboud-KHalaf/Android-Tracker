package com.example.tracker.ui.common

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tracker.R
import com.example.tracker.domain.export.ExportFormat

/**
 * An "Export" icon button that offers PDF or Excel, asks where to save the file, then calls
 * [onExport] with the chosen format and file URI. Shows progress instead while [isExporting].
 */
@Composable
fun ExportButton(
    isExporting: Boolean,
    fileName: (ExportFormat) -> String,
    onExport: (ExportFormat, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val launch = rememberExportLauncher(fileName, onExport)
    Box(modifier) {
        IconButton(onClick = { expanded = true }, enabled = !isExporting) {
            if (isExporting) {
                val description = stringResource(R.string.export_in_progress)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(ProgressSize)
                        .semantics { contentDescription = description },
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.export_action))
            }
        }
        ExportFormatMenu(expanded = expanded, onDismiss = { expanded = false }, onSelect = launch)
    }
}

/** The PDF / Excel choice, anchored to whatever it's placed beside in a [Box]. */
@Composable
fun ExportFormatMenu(expanded: Boolean, onDismiss: () -> Unit, onSelect: (ExportFormat) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        ExportFormat.entries.forEach { format ->
            DropdownMenuItem(
                text = { Text(stringResource(format.label)) },
                leadingIcon = { Icon(format.icon, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onSelect(format)
                },
            )
        }
    }
}

/**
 * Returns a function that opens the system "Save as" picker for a format, suggesting
 * [fileName]. When the user picks a place, [onExport] gets the format and the file's URI;
 * cancelling does nothing.
 */
@Composable
fun rememberExportLauncher(
    fileName: (ExportFormat) -> String,
    onExport: (ExportFormat, String) -> Unit,
): (ExportFormat) -> Unit {
    val currentOnExport by rememberUpdatedState(onExport)
    val launchers = ExportFormat.entries.associateWith { format ->
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mimeType)) { uri ->
            if (uri != null) currentOnExport(format, uri.toString())
        }
    }
    return { format ->
        try {
            launchers.getValue(format).launch(fileName(format))
        } catch (_: ActivityNotFoundException) {
            // No file picker on this device; nothing to save to.
        }
    }
}

private val ProgressSize = 20.dp

private val ExportFormat.label: Int
    get() = when (this) {
        ExportFormat.PDF -> R.string.export_pdf
        ExportFormat.XLSX -> R.string.export_excel
    }

private val ExportFormat.icon
    get() = when (this) {
        ExportFormat.PDF -> Icons.Outlined.PictureAsPdf
        ExportFormat.XLSX -> Icons.Outlined.TableChart
    }
