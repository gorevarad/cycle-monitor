package com.cyclemonitor.app.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.di.ViewModelFactory
import com.cyclemonitor.app.map.StaticRoutePreview
import com.cyclemonitor.app.ride.ui.Formatters
import com.cyclemonitor.app.theme.CycleColors
import java.util.Locale

@Composable
fun RideDetailScreen(container: AppContainer, rideId: String) {
    val viewModel: RideDetailViewModel = viewModel(
        factory = ViewModelFactory { RideDetailViewModel(container.rideRepository, container.settingsRepository, rideId) },
    )
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val current = detail
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading ride…", color = CycleColors.TextSecondary)
        }
        return
    }

    val summary = current.summary
    val points = current.trackPoints
    val routeCoordinates = points.map { it.sample.latitude to it.sample.longitude }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(summary.name.uppercase(Locale.getDefault()), style = MaterialTheme.typography.headlineSmall, color = CycleColors.TextPrimary)
            Text("${points.size} recorded points", style = MaterialTheme.typography.bodySmall, color = CycleColors.TextSecondary)
        }
        item {
            StaticRoutePreview(
                points = routeCoordinates,
                color = CycleColors.NavigationCyan,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(vertical = 16.dp),
            )
        }
        item {
            SummaryGrid(
                distance = Formatters.distance(settings.distanceUnit.fromMeters(summary.distanceMeters), settings.distanceUnit.symbol()),
                movingTime = Formatters.duration(summary.movingTimeSeconds),
                totalTime = Formatters.duration(summary.totalTimeSeconds),
                avgSpeed = Formatters.speed(summary.averageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) }, settings.speedUnit.symbol()),
                maxSpeed = Formatters.speed(summary.maxSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) }, settings.speedUnit.symbol()),
                elevationGain = Formatters.elevation(summary.elevationGainMeters?.let { settings.elevationUnit.fromMeters(it) }, settings.elevationUnit.symbol()),
                maxElevation = Formatters.elevation(summary.maxElevationMeters?.let { settings.elevationUnit.fromMeters(it) }, settings.elevationUnit.symbol()),
                avgGrade = Formatters.grade(summary.averageGradePercent),
                maxGrade = Formatters.grade(summary.maxGradePercent),
                avgPower = Formatters.power(summary.estimatedAveragePowerWatts),
                maxPower = Formatters.power(summary.estimatedMaxPowerWatts),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 16.dp)) {
                LineChart(
                    title = "SPEED VS TIME",
                    points = points.map { settings.speedUnit.fromMetersPerSecond(it.speedMps).toFloat() },
                    valueFormatter = { "%.1f %s".format(it, settings.speedUnit.symbol()) },
                    color = CycleColors.NavigationCyan,
                )
                LineChart(
                    title = "ELEVATION VS DISTANCE",
                    points = points.map { it.sample.altitudeMeters?.toFloat() },
                    valueFormatter = { "%.0f %s".format(it, settings.elevationUnit.symbol()) },
                    color = CycleColors.StatusGreen,
                )
                LineChart(
                    title = "ESTIMATED POWER VS TIME",
                    points = points.map { it.estimatedPowerWatts?.toFloat() },
                    valueFormatter = { "EST. %.0f W".format(it) },
                    color = CycleColors.StatusOrange,
                )
                LineChart(
                    title = "GRADIENT VS DISTANCE",
                    points = points.map { it.gradePercent?.toFloat() },
                    valueFormatter = { "%.1f%%".format(it) },
                    color = CycleColors.StatusAmber,
                )
            }
        }
    }
}

@Composable
private fun SummaryGrid(
    distance: String,
    movingTime: String,
    totalTime: String,
    avgSpeed: String,
    maxSpeed: String,
    elevationGain: String,
    maxElevation: String,
    avgGrade: String,
    maxGrade: String,
    avgPower: String,
    maxPower: String,
) {
    val entries = listOf(
        "DISTANCE" to distance,
        "MOVING TIME" to movingTime,
        "TOTAL TIME" to totalTime,
        "AVG SPEED" to avgSpeed,
        "MAX SPEED" to maxSpeed,
        "ELEVATION GAIN" to elevationGain,
        "MAX ELEVATION" to maxElevation,
        "AVG GRADE" to avgGrade,
        "MAX GRADE" to maxGrade,
        "EST. AVG POWER" to avgPower,
        "EST. MAX POWER" to maxPower,
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.chunked(3).forEach { rowEntries ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowEntries.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(value, style = MaterialTheme.typography.titleSmall, color = CycleColors.TextPrimary)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = CycleColors.TextSecondary)
                    }
                }
                repeat(3 - rowEntries.size) { Column(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}
