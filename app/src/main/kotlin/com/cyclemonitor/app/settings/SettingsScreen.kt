package com.cyclemonitor.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclemonitor.app.BuildConfig
import com.cyclemonitor.app.data.settings.DataRetention
import com.cyclemonitor.app.data.settings.MapStyle
import com.cyclemonitor.app.data.settings.ThemePreference
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.di.ViewModelFactory
import com.cyclemonitor.app.map.MapAvailability
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.location.LocationAccuracyMode
import com.cyclemonitor.core.model.AnimationIntensity
import com.cyclemonitor.core.model.BikeType
import com.cyclemonitor.core.model.RidingPosition
import com.cyclemonitor.core.units.DistanceUnit
import com.cyclemonitor.core.units.ElevationUnit
import com.cyclemonitor.core.units.SpeedUnit

@Composable
fun SettingsScreen(container: AppContainer, onOpenDashboardCustomization: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory { SettingsViewModel(container.settingsRepository) })
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dashboardProfiles by container.dashboardProfileRepository.observeProfiles().collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineSmall, color = CycleColors.TextPrimary, modifier = Modifier.padding(top = 16.dp))

        SettingsSectionHeader("GENERAL")
        SettingsChoiceRow("Speed unit", SpeedUnit.entries, settings.speedUnit, { it.symbol() }, viewModel::setSpeedUnit)
        SettingsChoiceRow("Distance unit", DistanceUnit.entries, settings.distanceUnit, { it.symbol() }, viewModel::setDistanceUnit)
        SettingsChoiceRow("Elevation unit", ElevationUnit.entries, settings.elevationUnit, { it.symbol() }, viewModel::setElevationUnit)
        SettingsChoiceRow("Theme", ThemePreference.entries, settings.theme, { it.name }, viewModel::setTheme)

        SettingsSectionHeader("DASHBOARD")
        if (dashboardProfiles.isNotEmpty()) {
            SettingsChoiceRow(
                "Profile",
                dashboardProfiles,
                dashboardProfiles.find { it.id == settings.dashboardProfileId } ?: dashboardProfiles.first(),
                { it.name },
                { viewModel.setDashboardProfileId(it.id) },
            )
        }
        SettingsChoiceRow("Animation intensity", AnimationIntensity.entries, settings.animationIntensity, { it.name }, viewModel::setAnimationIntensity)
        Button(onClick = onOpenDashboardCustomization, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("CUSTOMIZE DASHBOARD")
        }

        SettingsSectionHeader("POWER")
        Text(
            "Power is estimated from speed, gradient, and these parameters using a physics model -- it is not a substitute for a physical power meter.",
            style = MaterialTheme.typography.bodySmall,
            color = CycleColors.TextSecondary,
        )
        SettingsSliderRow(
            "Rider weight",
            settings.riderProfile.riderWeightKg.toFloat(),
            30f..150f,
            "${settings.riderProfile.riderWeightKg.toInt()} kg",
        ) { viewModel.setRiderWeightKg(it.toDouble()) }
        SettingsSliderRow(
            "Bike weight",
            settings.riderProfile.bikeWeightKg.toFloat(),
            5f..25f,
            "${settings.riderProfile.bikeWeightKg.toInt()} kg",
        ) { viewModel.setBikeWeightKg(it.toDouble()) }
        SettingsChoiceRow("Bike type", BikeType.entries, settings.riderProfile.bikeType, { it.name }, viewModel::setBikeType)
        SettingsChoiceRow("Riding position", RidingPosition.entries, settings.riderProfile.ridingPosition, { it.name }, viewModel::setRidingPosition)

        SettingsSectionHeader("GPS")
        SettingsChoiceRow("Accuracy mode", LocationAccuracyMode.entries, settings.locationAccuracyMode, { it.name }, viewModel::setLocationAccuracyMode)
        Text(
            "Higher accuracy improves GPS quality but uses more battery.",
            style = MaterialTheme.typography.bodySmall,
            color = CycleColors.TextSecondary,
        )

        SettingsSectionHeader("RIDE")
        SettingsSwitchRow("Auto-pause when stopped", settings.autoPauseEnabled, viewModel::setAutoPauseEnabled)
        SettingsChoiceRow("Data retention", DataRetention.entries, settings.dataRetention, { it.label }, viewModel::setDataRetention)

        SettingsSectionHeader("NAVIGATION")
        SettingsChoiceRow("Map style", MapStyle.entries, settings.mapStyle, { it.name }, viewModel::setMapStyle)
        Text(
            when {
                !MapAvailability.isConfigured -> "No Maps API key configured -- map shows an unavailable state."
                else -> "Google Maps is configured. Turn-by-turn routing uses the Directions API (bicycling mode) with the same key; enable the Directions API for it in Google Cloud Console."
            },
            style = MaterialTheme.typography.bodySmall,
            color = CycleColors.TextSecondary,
        )

        if (BuildConfig.ALLOW_MOCK_LOCATION) {
            SettingsSectionHeader("DEVELOPER")
            SettingsSwitchRow("Use simulated GPS (mock ride)", settings.useMockLocationForDevelopment, viewModel::setUseMockLocation)
            Text(
                "Debug builds only. Never available in a release build.",
                style = MaterialTheme.typography.bodySmall,
                color = CycleColors.TextDisabled,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}
