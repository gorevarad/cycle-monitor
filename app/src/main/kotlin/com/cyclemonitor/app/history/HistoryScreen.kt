package com.cyclemonitor.app.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.di.ViewModelFactory
import com.cyclemonitor.app.ride.ui.Formatters
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.RideSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(container: AppContainer, onOpenRide: (String) -> Unit) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = ViewModelFactory { HistoryViewModel(container.rideRepository, container.settingsRepository) },
    )
    val rides by viewModel.rides.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    if (rides.isEmpty()) {
        EmptyHistory()
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(rides, key = { it.id }) { ride ->
            RideCard(
                ride = ride,
                speedUnitSymbol = settings.speedUnit.symbol(),
                distanceUnitSymbol = settings.distanceUnit.symbol(),
                elevationUnitSymbol = settings.elevationUnit.symbol(),
                distanceValue = settings.distanceUnit.fromMeters(ride.distanceMeters),
                speedValue = ride.averageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) },
                elevationValue = ride.elevationGainMeters?.let { settings.elevationUnit.fromMeters(it) },
                onClick = { onOpenRide(ride.id) },
                onDelete = { viewModel.deleteRide(ride.id) },
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No rides recorded yet.\nStart a ride to see it here.",
            style = MaterialTheme.typography.bodyLarge,
            color = CycleColors.TextSecondary,
        )
    }
}

@Composable
private fun RideCard(
    ride: RideSummary,
    speedUnitSymbol: String,
    distanceUnitSymbol: String,
    elevationUnitSymbol: String,
    distanceValue: Double,
    speedValue: Double?,
    elevationValue: Double?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(CycleColors.SurfaceRaised, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(ride.name.uppercase(Locale.getDefault()), style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
                Text(dateFormat.format(Date(ride.startTimeMillis)), style = MaterialTheme.typography.bodySmall, color = CycleColors.TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ride", tint = CycleColors.TextDisabled)
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(Formatters.distance(distanceValue, distanceUnitSymbol), color = CycleColors.TextPrimary)
            Text(Formatters.duration(ride.movingTimeSeconds), color = CycleColors.TextPrimary)
            Text(Formatters.speed(speedValue, speedUnitSymbol), color = CycleColors.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(Formatters.elevation(elevationValue, elevationUnitSymbol), color = CycleColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Text(Formatters.power(ride.estimatedAveragePowerWatts), color = CycleColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
