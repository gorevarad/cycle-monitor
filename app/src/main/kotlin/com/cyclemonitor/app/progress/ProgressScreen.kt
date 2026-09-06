package com.cyclemonitor.app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.di.ViewModelFactory
import com.cyclemonitor.app.ride.ui.Formatters
import com.cyclemonitor.app.theme.CycleColors

@Composable
fun ProgressScreen(container: AppContainer, onOpenRecords: () -> Unit) {
    val viewModel: ProgressViewModel = viewModel(
        factory = ViewModelFactory { ProgressViewModel(container.rideRepository, container.settingsRepository) },
    )
    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val filter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("PROGRESS", style = MaterialTheme.typography.headlineSmall, color = CycleColors.TextPrimary)

        Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsFilter.entries.forEach { option ->
                FilterChip(
                    selected = option == filter,
                    onClick = { viewModel.selectFilter(option) },
                    label = { Text(option.label) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            StatRow("Distance", Formatters.distance(settings.distanceUnit.fromMeters(stats.distanceMeters), settings.distanceUnit.symbol()))
            StatRow("Ride time", Formatters.duration(stats.movingTimeSeconds))
            StatRow("Number of rides", stats.rideCount.toString())
            StatRow("Elevation gain", Formatters.elevation(stats.elevationGainMeters?.let { settings.elevationUnit.fromMeters(it) }, settings.elevationUnit.symbol()))
            StatRow("Average speed", Formatters.speed(stats.averageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) }, settings.speedUnit.symbol()))
        }

        Button(onClick = onOpenRecords, modifier = Modifier.padding(top = 24.dp)) {
            Text("VIEW PERSONAL RECORDS")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = CycleColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
    }
}
