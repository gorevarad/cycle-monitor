package com.cyclemonitor.app.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.core.model.RideSummary
import com.cyclemonitor.core.repository.RideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val rideRepository: RideRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val rides: StateFlow<List<RideSummary>> = rideRepository.observeRides()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun deleteRide(rideId: String) {
        viewModelScope.launch { rideRepository.deleteRide(rideId) }
    }

    fun renameRide(rideId: String, newName: String) {
        viewModelScope.launch { rideRepository.renameRide(rideId, newName) }
    }
}
