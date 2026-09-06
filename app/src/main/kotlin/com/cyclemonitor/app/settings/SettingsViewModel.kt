package com.cyclemonitor.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.data.settings.ThemePreference
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.core.location.LocationAccuracyMode
import com.cyclemonitor.core.model.AnimationIntensity
import com.cyclemonitor.core.model.BikeType
import com.cyclemonitor.core.model.RiderProfile
import com.cyclemonitor.core.model.RidingPosition
import com.cyclemonitor.core.units.DistanceUnit
import com.cyclemonitor.core.units.ElevationUnit
import com.cyclemonitor.core.units.SpeedUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setSpeedUnit(unit: SpeedUnit) = viewModelScope.launch { repository.updateSpeedUnit(unit) }
    fun setDistanceUnit(unit: DistanceUnit) = viewModelScope.launch { repository.updateDistanceUnit(unit) }
    fun setElevationUnit(unit: ElevationUnit) = viewModelScope.launch { repository.updateElevationUnit(unit) }
    fun setTheme(theme: ThemePreference) = viewModelScope.launch { repository.updateTheme(theme) }
    fun setAnimationIntensity(intensity: AnimationIntensity) = viewModelScope.launch { repository.updateAnimationIntensity(intensity) }
    fun setLocationAccuracyMode(mode: LocationAccuracyMode) = viewModelScope.launch { repository.updateLocationAccuracyMode(mode) }
    fun setAutoPauseEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateAutoPauseEnabled(enabled) }
    fun setDashboardProfileId(id: String) = viewModelScope.launch { repository.updateDashboardProfileId(id) }
    fun setUseMockLocation(enabled: Boolean) = viewModelScope.launch { repository.updateUseMockLocationForDevelopment(enabled) }

    fun setRiderWeightKg(kg: Double) = updateRiderProfile { it.copy(riderWeightKg = kg) }
    fun setBikeWeightKg(kg: Double) = updateRiderProfile { it.copy(bikeWeightKg = kg) }
    fun setBikeType(type: BikeType) = updateRiderProfile { it.copy(bikeType = type) }
    fun setRidingPosition(position: RidingPosition) = updateRiderProfile { it.copy(ridingPosition = position) }

    private fun updateRiderProfile(transform: (RiderProfile) -> RiderProfile) {
        viewModelScope.launch {
            val current = settings.value.riderProfile
            repository.updateRiderProfile(transform(current))
        }
    }
}
