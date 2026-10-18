package com.example.tracker.ui.plan.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.ui.common.DestructiveConfirmDialog

/** Confirms deleting the training day; finished workouts are kept. */
@Composable
fun DeletePlanDialog(
    planName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DestructiveConfirmDialog(
        title = stringResource(R.string.plan_delete_title, planName),
        body = stringResource(R.string.plan_delete_body),
        confirmLabel = stringResource(R.string.plan_delete_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
