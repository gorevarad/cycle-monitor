package com.cyclemonitor.app.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.core.repository.RideRepository
import com.cyclemonitor.core.stats.PersonalRecords
import com.cyclemonitor.core.stats.PersonalRecordsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PersonalRecordsViewModel(
    rideRepository: RideRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val records: StateFlow<PersonalRecords> = rideRepository.observeRides()
        .map { PersonalRecordsCalculator.calculate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalRecords.EMPTY)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())
}
