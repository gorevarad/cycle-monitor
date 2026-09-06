package com.cyclemonitor.app.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun PersonalRecordsScreen(container: AppContainer) {
    val viewModel: PersonalRecordsViewModel = viewModel(
        factory = ViewModelFactory { PersonalRecordsViewModel(container.rideRepository, container.settingsRepository) },
    )
    val records by viewModel.records.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("PERSONAL RECORDS", style = MaterialTheme.typography.headlineSmall, color = CycleColors.TextPrimary)

        RecordRow("Longest ride", Formatters.distance(records.longestRideDistanceMeters?.let { settings.distanceUnit.fromMeters(it) }, settings.distanceUnit.symbol()))
        RecordRow("Longest ride duration", records.longestRideDurationSeconds?.let { Formatters.duration(it) } ?: "N/A")
        RecordRow("Highest elevation gain", Formatters.elevation(records.highestElevationGainMeters?.let { settings.elevationUnit.fromMeters(it) }, settings.elevationUnit.symbol()))
        RecordRow("Highest average speed", Formatters.speed(records.highestAverageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) }, settings.speedUnit.symbol()))
        RecordRow("Highest max speed", Formatters.speed(records.highestMaxSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) }, settings.speedUnit.symbol()))
        RecordRow("Highest estimated power", Formatters.power(records.highestEstimatedPowerWatts))

        Text(
            "Records are calculated only from complete, valid rides.",
            style = MaterialTheme.typography.bodySmall,
            color = CycleColors.TextDisabled,
        )
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = CycleColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
    }
}
