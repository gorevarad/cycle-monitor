package com.cyclemonitor.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Ride : Screen("ride")
    data object History : Screen("history")
    data object Progress : Screen("progress")
    data object PersonalRecords : Screen("records")
    data object Flex : Screen("flex")
    data object Settings : Screen("settings")
    data object DashboardCustomization : Screen("dashboard_customization")
    data object RideDetail : Screen("ride_detail/{rideId}") {
        fun createRoute(rideId: String) = "ride_detail/$rideId"
    }
}

data class BottomDestination(val screen: Screen, val label: String, val icon: ImageVector)

val bottomDestinations = listOf(
    BottomDestination(Screen.Ride, "Ride", Icons.Filled.DirectionsBike),
    BottomDestination(Screen.History, "History", Icons.Filled.History),
    BottomDestination(Screen.Progress, "Progress", Icons.Filled.BarChart),
    BottomDestination(Screen.Flex, "Flex", Icons.Filled.MovieFilter),
    BottomDestination(Screen.Settings, "Settings", Icons.Filled.Settings),
)
