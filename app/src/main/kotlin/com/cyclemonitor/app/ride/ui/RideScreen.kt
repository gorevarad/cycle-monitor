package com.cyclemonitor.app.ride.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.di.ViewModelFactory
import com.cyclemonitor.app.map.RideMapPanel
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.DashboardMetric

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun RideScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel: RideViewModel = viewModel(
        factory = ViewModelFactory { RideViewModel(context.applicationContext, container.settingsRepository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val position by viewModel.riderPosition.collectAsStateWithLifecycle()

    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }

    if (!permissionGranted) {
        LocationPermissionRequest(onRequest = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
        return
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        RideDashboardLandscape(state, position, viewModel)
    } else {
        RideDashboardPortrait(state, position, viewModel)
    }
}

@Composable
private fun LocationPermissionRequest(onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Cycle Monitor needs location access to track your rides.",
                style = MaterialTheme.typography.bodyLarge,
                color = CycleColors.TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequest) { Text("Grant location access") }
        }
    }
}

@Composable
private fun RideDashboardLandscape(
    state: RideUiState,
    position: com.cyclemonitor.app.map.RiderPosition?,
    viewModel: RideViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (DashboardMetric.SPEED in state.visibleMetrics) {
                SpeedGauge(
                    displaySpeed = state.displaySpeed,
                    unitSymbol = state.speedUnit.symbol(),
                    maxScale = state.speedGaugeMaxKmh,
                    averageSpeed = state.averageSpeed,
                    maxSpeed = state.maxSpeed,
                    animationIntensity = state.animationIntensity,
                    modifier = Modifier.weight(1f).padding(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1.3f).padding(16.dp)) {
                RideMapPanel(position = position, modifier = Modifier.weight(1f))
                Spacer(Modifier.height(12.dp))
                RideControls(
                    state = state.rideState,
                    onStart = viewModel::onStartRide,
                    onPause = viewModel::onPauseRide,
                    onResume = viewModel::onResumeRide,
                    onFinish = viewModel::onFinishRide,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            if (DashboardMetric.ESTIMATED_POWER in state.visibleMetrics) {
                PowerGauge(
                    displayPowerWatts = state.displayPowerWatts,
                    confidence = state.powerConfidence,
                    maxScaleWatts = state.powerGaugeMaxWatts,
                    power3sAvgWatts = state.power3sAvgWatts,
                    averagePowerWatts = state.averagePowerWatts,
                    maxPowerWatts = state.maxPowerWatts,
                    animationIntensity = state.animationIntensity,
                    modifier = Modifier.weight(1f).padding(16.dp),
                )
            }
        }
        MetricsBar(state)
    }
}

@Composable
private fun RideDashboardPortrait(
    state: RideUiState,
    position: com.cyclemonitor.app.map.RiderPosition?,
    viewModel: RideViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RideMapPanel(position = position, modifier = Modifier.fillMaxWidth().weight(1f))
        Row(modifier = Modifier.fillMaxWidth().weight(1.2f)) {
            SpeedGauge(
                displaySpeed = state.displaySpeed,
                unitSymbol = state.speedUnit.symbol(),
                maxScale = state.speedGaugeMaxKmh,
                averageSpeed = state.averageSpeed,
                maxSpeed = state.maxSpeed,
                animationIntensity = state.animationIntensity,
                modifier = Modifier.weight(1f).padding(12.dp),
            )
            PowerGauge(
                displayPowerWatts = state.displayPowerWatts,
                confidence = state.powerConfidence,
                maxScaleWatts = state.powerGaugeMaxWatts,
                power3sAvgWatts = state.power3sAvgWatts,
                averagePowerWatts = state.averagePowerWatts,
                maxPowerWatts = state.maxPowerWatts,
                animationIntensity = state.animationIntensity,
                modifier = Modifier.weight(1f).padding(12.dp),
            )
        }
        RideControls(
            state = state.rideState,
            onStart = viewModel::onStartRide,
            onPause = viewModel::onPauseRide,
            onResume = viewModel::onResumeRide,
            onFinish = viewModel::onFinishRide,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
        )
        MetricsBar(state)
    }
}
