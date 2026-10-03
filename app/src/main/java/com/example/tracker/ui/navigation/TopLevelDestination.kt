package com.example.tracker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.tracker.R
import com.example.tracker.ui.exercises.ExerciseListDestination
import com.example.tracker.ui.history.HistoryDestination
import com.example.tracker.ui.home.HomeDestination
import com.example.tracker.ui.nutrition.NutritionDestination
import com.example.tracker.ui.weight.WeightDestination
import kotlin.reflect.KClass

/** The screens in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeDestination, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY(HistoryDestination, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History),
    PROGRESS(ExerciseListDestination, R.string.nav_progress, Icons.Filled.Insights, Icons.Outlined.Insights),
    NUTRITION(NutritionDestination, R.string.nav_nutrition, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    WEIGHT(WeightDestination, R.string.nav_weight, Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight);

    private val routeClass: KClass<*> get() = route::class

    /** Whether [destination] is this tab's screen. */
    fun matches(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { it.hasRoute(routeClass) } == true

    companion object {
        /** The tab whose screen [destination] is, or null on any other screen. */
        fun of(destination: NavDestination?): TopLevelDestination? = entries.firstOrNull { it.matches(destination) }
    }
}

/**
 * Switches tab the standard way: one copy of each tab on the back stack, Back from a tab
 * returns to Home, and each tab keeps its scroll position and state.
 */
fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
