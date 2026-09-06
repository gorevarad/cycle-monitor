package com.cyclemonitor.app.ride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors

/** The dashboard's bottom row: Distance / Time / Elevation / Grade / GPS, per the reference sketch. */
@Composable
fun MetricsBar(state: RideUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MetricCell("DISTANCE", Formatters.distance(state.distance, state.distanceUnit.symbol()))
        MetricCell("TIME", Formatters.duration(state.elapsedTimeSeconds))
        MetricCell("ELEVATION", Formatters.elevation(state.elevationGain, state.elevationUnit.symbol()))
        MetricCell("GRADE", Formatters.grade(state.gradePercent))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GpsStatusIndicator(quality = state.gpsQuality, accuracyMeters = state.gpsAccuracyMeters)
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CycleColors.TextSecondary)
    }
}
