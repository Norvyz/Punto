package com.punto.app.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.punto.app.presentation.history.HistoryScreen
import com.punto.app.presentation.home.HomeScreen
import com.punto.app.presentation.map.MapScreen
import com.punto.app.presentation.reminders.RemindersScreen
import com.punto.app.presentation.settings.SettingsScreen

@Composable
fun PuntoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onCreateReminder: () -> Unit = {},
    onEditReminder: (String) -> Unit = {},
    onShowTutorial: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = PuntoDestination.HOME.route,
        modifier = modifier,
        enterTransition = { fadeIn() + slideInHorizontally { it / 24 } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() + slideOutHorizontally { it / 24 } },
    ) {
        composable(PuntoDestination.HOME.route) { HomeScreen(onCreateReminder = onCreateReminder, onSeeAll = { navController.navigate(PuntoDestination.REMINDERS.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }) }
        composable(PuntoDestination.MAP.route) { MapScreen() }
        composable(PuntoDestination.REMINDERS.route) { RemindersScreen(onCreateReminder = onCreateReminder, onEditReminder = onEditReminder) }
        composable(PuntoDestination.HISTORY.route) { HistoryScreen() }
        composable(PuntoDestination.SETTINGS.route) { SettingsScreen(onShowTutorial = onShowTutorial) }
    }
}
