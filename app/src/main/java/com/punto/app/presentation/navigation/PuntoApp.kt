package com.punto.app.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.punto.app.presentation.createreminder.CreateReminderScreen
import com.punto.app.presentation.setup.SetupScreen

private const val NAV_ANIM_MS = 320

@Composable
fun PuntoApp(onboardingCompleted: Boolean) {
    val rootNavController = rememberNavController()
    // Se decide una sola vez: si cambiara con el estado, NavHost reiniciaría el grafo.
    val startDestination = remember { if (onboardingCompleted) PuntoRoutes.MAIN else PuntoRoutes.SETUP }

    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(NAV_ANIM_MS)) + slideInHorizontally(tween(NAV_ANIM_MS)) { it / 8 } },
        exitTransition = { fadeOut(tween(NAV_ANIM_MS / 2)) },
        popEnterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
        popExitTransition = { fadeOut(tween(NAV_ANIM_MS / 2)) + slideOutHorizontally(tween(NAV_ANIM_MS)) { it / 8 } },
    ) {
        composable(PuntoRoutes.SETUP) {
            SetupScreen(
                onFinished = {
                    rootNavController.navigate(PuntoRoutes.MAIN) { popUpTo(PuntoRoutes.SETUP) { inclusive = true } }
                },
            )
        }
        composable(PuntoRoutes.MAIN) {
            MainScaffold(
                onCreateReminder = { rootNavController.navigate(PuntoRoutes.CREATE_REMINDER) },
                onEditReminder = { id -> rootNavController.navigate(PuntoRoutes.editReminder(id)) },
            )
        }
        composable(PuntoRoutes.CREATE_REMINDER) {
            CreateReminderScreen(onBack = { rootNavController.popBackStack() })
        }
        composable(
            route = PuntoRoutes.EDIT_REMINDER,
            arguments = listOf(navArgument("reminderId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) {
            CreateReminderScreen(onBack = { rootNavController.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(onCreateReminder: () -> Unit, onEditReminder: (String) -> Unit) {
    val navController = rememberNavController()
    val destinations = PuntoDestination.entries
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "navIconScale",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = null,
                                modifier = Modifier.scale(iconScale),
                            )
                        },
                        // Una sola línea siempre: etiquetas cortas + sin salto de línea.
                        label = {
                            Text(
                                stringResource(destination.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        PuntoNavHost(navController = navController, modifier = Modifier.padding(innerPadding), onCreateReminder = onCreateReminder, onEditReminder = onEditReminder)
    }
}
