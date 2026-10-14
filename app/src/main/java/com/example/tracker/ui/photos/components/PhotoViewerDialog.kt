package com.example.tracker.ui.photos.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.tracker.R
import com.example.tracker.domain.model.ProgressPhoto
import com.example.tracker.ui.common.currentLocale
import com.example.tracker.ui.common.dateWithYear
import com.example.tracker.ui.photos.labelRes
import com.example.tracker.ui.theme.spacing
import java.io.File

/** One photo full screen, with its pose and date, and a confirmed delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerDialog(
    photo: ProgressPhoto,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val poseName = stringResource(photo.pose.labelRes)
    val date = photo.date.dateWithYear(currentLocale())
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                TopAppBar(
                    title = { Text(poseName) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.photos_close))
                        }
                    },
                    actions = {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    },
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(MaterialTheme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = stringResource(R.string.photos_photo_description, poseName, date),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.photos_delete_title)) },
            text = { Text(stringResource(R.string.photos_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
