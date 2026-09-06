package com.cyclemonitor.app.flex

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
fun FlexScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel: FlexViewModel = viewModel(
        factory = ViewModelFactory { FlexViewModel(container.rideRepository, container.settingsRepository) },
    )
    val rides by viewModel.availableRides.collectAsStateWithLifecycle()
    val selectedRideId by viewModel.selectedRideId.collectAsStateWithLifecycle()
    val detail by viewModel.selectedRideDetail.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    LaunchedEffect(rides) {
        if (selectedRideId == null && rides.isNotEmpty()) viewModel.selectRide(rides.first().id)
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                Toast.makeText(context, "Saved to ${state.uri}", Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            is ExportState.Failure -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("FLEX MODE", style = MaterialTheme.typography.headlineSmall, color = CycleColors.TextPrimary)
        Text(
            "Transparent overlays for your ride videos and photos.",
            style = MaterialTheme.typography.bodySmall,
            color = CycleColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (rides.isEmpty()) {
            Text("Complete a ride first to create a Flex overlay.", color = CycleColors.TextSecondary)
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            FlexPreset.entries.forEach { preset ->
                FilterChip(selected = config.preset == preset, onClick = { viewModel.selectPreset(preset) }, label = { Text(preset.name) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            FlexAspectRatio.entries.forEach { ratio ->
                FilterChip(
                    selected = config.aspectRatio == ratio,
                    onClick = { viewModel.updateConfig { it.copy(aspectRatio = ratio) } },
                    label = { Text(ratio.label) },
                )
            }
        }

        // Checkerboard behind the preview signals "this area is transparent" -- it is not part
        // of the exported PNG, which is produced independently by FlexPngExporter.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(config.aspectRatio.widthRatio / config.aspectRatio.heightRatio)
                .padding(bottom = 16.dp),
        ) {
            CheckerboardBackground(modifier = Modifier.fillMaxSize())
            detail?.let { d ->
                FlexPreviewContent(
                    config = config,
                    summary = d.summary,
                    routePoints = d.trackPoints.map { it.sample.latitude to it.sample.longitude },
                    speedUnitSymbol = settings.speedUnit.symbol(),
                    distanceUnitSymbol = settings.distanceUnit.symbol(),
                    elevationUnitSymbol = settings.elevationUnit.symbol(),
                    distanceValue = settings.distanceUnit.fromMeters(d.summary.distanceMeters),
                    speedValue = d.summary.averageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) },
                    elevationValue = d.summary.elevationGainMeters?.let { settings.elevationUnit.fromMeters(it) },
                )
            }
        }

        Text("Route opacity", color = CycleColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        Slider(value = config.routeOpacity, onValueChange = { v -> viewModel.updateConfig { it.copy(routeOpacity = v, preset = FlexPreset.CUSTOM) } })

        Text("Text opacity", color = CycleColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        Slider(value = config.textOpacity, onValueChange = { v -> viewModel.updateConfig { it.copy(textOpacity = v, preset = FlexPreset.CUSTOM) } })

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Show ride name", color = CycleColors.TextPrimary)
            Switch(checked = config.showRideName, onCheckedChange = { v -> viewModel.updateConfig { it.copy(showRideName = v, preset = FlexPreset.CUSTOM) } })
        }

        Button(
            onClick = {
                val longEdgePx = with(density) { EXPORT_LONG_EDGE_DP.dp.toPx() }.toInt()
                val ratio = config.aspectRatio
                val (widthPx, heightPx) = if (ratio.heightRatio >= ratio.widthRatio) {
                    (longEdgePx * ratio.widthRatio / ratio.heightRatio).toInt() to longEdgePx
                } else {
                    longEdgePx to (longEdgePx * ratio.heightRatio / ratio.widthRatio).toInt()
                }
                viewModel.export(context, widthPx, heightPx)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
            enabled = exportState !is ExportState.Exporting,
        ) {
            Text(if (exportState is ExportState.Exporting) "EXPORTING..." else "EXPORT TRANSPARENT PNG")
        }
    }
}

private const val EXPORT_LONG_EDGE_DP = 1080

@Composable
private fun FlexPreviewContent(
    config: FlexConfig,
    summary: com.cyclemonitor.core.model.RideSummary,
    routePoints: List<Pair<Double, Double>>,
    speedUnitSymbol: String,
    distanceUnitSymbol: String,
    elevationUnitSymbol: String,
    distanceValue: Double,
    speedValue: Double?,
    elevationValue: Double?,
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        if (config.showRoute && routePoints.size >= 2) {
            StaticRoutePreview(
                points = routePoints,
                color = Color.Cyan.copy(alpha = config.routeOpacity),
                modifier = Modifier.fillMaxSize(),
                strokeWidthPx = config.routeStrokeWidthPx,
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            val textColor = Color.White.copy(alpha = config.textOpacity)
            if (config.showRideName) Text(summary.name.uppercase(Locale.US), color = textColor, style = MaterialTheme.typography.titleMedium)
            if (config.showDistance) Text(Formatters.distance(distanceValue, distanceUnitSymbol), color = textColor, style = MaterialTheme.typography.headlineSmall)
            if (config.showSpeed) Text(Formatters.speed(speedValue, speedUnitSymbol), color = textColor, style = MaterialTheme.typography.titleLarge)
            if (config.showTime) Text(Formatters.duration(summary.movingTimeSeconds), color = textColor, style = MaterialTheme.typography.titleLarge)
            if (config.showElevation) Text(Formatters.elevation(elevationValue, elevationUnitSymbol), color = textColor, style = MaterialTheme.typography.titleMedium)
            if (config.showPower) Text(Formatters.power(summary.estimatedAveragePowerWatts), color = textColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(CycleColors.SurfaceCharcoal)) {
        val cell = 16f
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else cell
            while (x < size.width) {
                drawRect(color = CycleColors.SurfaceRaised, topLeft = Offset(x, y), size = Size(cell, cell))
                x += cell * 2
            }
            y += cell
            row++
        }
    }
}
