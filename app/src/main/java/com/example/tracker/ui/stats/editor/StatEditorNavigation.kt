package com.example.tracker.ui.stats.editor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.tracker.ui.stats.StatTemplate
import kotlinx.serialization.Serializable

/** Edits the stat [statId], or creates one, starting from [template] (a [StatTemplate] name) when given. */
@Serializable
data class StatEditorDestination(val statId: String? = null, val template: String? = null)

fun NavController.navigateToNewStat(template: StatTemplate? = null) =
    navigate(StatEditorDestination(template = template?.name)) { launchSingleTop = true }

fun NavController.navigateToEditStat(statId: String) =
    navigate(StatEditorDestination(statId = statId)) { launchSingleTop = true }

/** [onCreated] gets the new stat's id; [onDeleted] follows deleting the stat being edited. */
fun NavGraphBuilder.statEditorScreen(
    onClose: () -> Unit,
    onCreated: (statId: String) -> Unit,
    onDeleted: () -> Unit,
) {
    composable<StatEditorDestination> {
        StatEditorRoute(onClose = onClose, onCreated = onCreated, onDeleted = onDeleted)
    }
}
