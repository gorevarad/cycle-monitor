package com.cyclemonitor.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyclemonitor.app.dashboard.DashboardCustomizationScreen
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.flex.FlexScreen
import com.cyclemonitor.app.history.HistoryScreen
import com.cyclemonitor.app.history.RideDetailScreen
import com.cyclemonitor.app.progress.ProgressScreen
import com.cyclemonitor.app.records.PersonalRecordsScreen
import com.cyclemonitor.app.ride.service.RideRecordingService
import com.cyclemonitor.app.ride.ui.RideScreen
import com.cyclemonitor.app.settings.SettingsScreen
import com.cyclemonitor.core.ride.RideState

@Composable
fun CycleMonitorNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val rideState by RideRecordingService.rideState.collectAsStateWithLifecycle()
    // A ride in progress hides the bottom nav so a rider can't accidentally leave the ride
    // screen mid-recording (spec: "navigation should be minimized" during an active ride).
    val isRideActive = rideState == RideState.Riding || rideState == RideState.Paused

    Scaffold(
        bottomBar = {
            if (!isRideActive) {
                CycleBottomNavigation(navController)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Ride.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Ride.route) {
                RideScreen(container, onNavigateToRideDetail = { rideId ->
                    navController.navigate(Screen.RideDetail.createRoute(rideId))
                })
            }
            composable(Screen.History.route) {
                HistoryScreen(container, onOpenRide = { rideId ->
                    navController.navigate(Screen.RideDetail.createRoute(rideId))
                })
            }
            composable(Screen.RideDetail.route) { backStackEntry ->
                val rideId = backStackEntry.arguments?.getString("rideId").orEmpty()
                RideDetailScreen(container, rideId)
            }
            composable(Screen.Progress.route) {
                ProgressScreen(container, onOpenRecords = { navController.navigate(Screen.PersonalRecords.route) })
            }
            composable(Screen.PersonalRecords.route) { PersonalRecordsScreen(container) }
            composable(Screen.Flex.route) { FlexScreen(container) }
            composable(Screen.Settings.route) {
                SettingsScreen(container, onOpenDashboardCustomization = { navController.navigate(Screen.DashboardCustomization.route) })
            }
            composable(Screen.DashboardCustomization.route) { DashboardCustomizationScreen(container) }
        }
    }
}

@Composable
private fun CycleBottomNavigation(navController: androidx.navigation.NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        bottomDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}
