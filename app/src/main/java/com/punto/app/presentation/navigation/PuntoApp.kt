package com.punto.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.punto.app.presentation.createreminder.CreateReminderScreen
import com.punto.app.presentation.onboarding.OnboardingScreen
import com.punto.app.presentation.permissions.PermissionsScreen
import com.punto.app.presentation.welcome.WelcomeScreen

@Composable
fun PuntoApp() {
    val rootNavController = rememberNavController()
    NavHost(navController = rootNavController, startDestination = PuntoRoutes.WELCOME) {
        composable(PuntoRoutes.WELCOME) {
            WelcomeScreen(
                onStart = { rootNavController.navigate(PuntoRoutes.ONBOARDING) },
                onSkip = { rootNavController.navigate(PuntoRoutes.MAIN) { popUpTo(PuntoRoutes.WELCOME) { inclusive = true } } },
            )
        }
        composable(PuntoRoutes.ONBOARDING) {
            OnboardingScreen(onFinish = { rootNavController.navigate(PuntoRoutes.PERMISSIONS) { popUpTo(PuntoRoutes.WELCOME) { inclusive = true } } })
        }
        composable(PuntoRoutes.PERMISSIONS) {
            PermissionsScreen(onContinue = { rootNavController.navigate(PuntoRoutes.MAIN) { popUpTo(PuntoRoutes.WELCOME) { inclusive = true } } })
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
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) destination.selectedIcon else destination.unselectedIcon, contentDescription = stringResource(destination.labelRes)) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        PuntoNavHost(navController = navController, modifier = Modifier.padding(innerPadding), onCreateReminder = onCreateReminder, onEditReminder = onEditReminder)
    }
}
