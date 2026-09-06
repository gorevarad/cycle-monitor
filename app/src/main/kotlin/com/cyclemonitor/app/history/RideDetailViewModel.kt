package com.cyclemonitor.app.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.core.model.RideDetail
import com.cyclemonitor.core.repository.RideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideDetailViewModel(
    rideRepository: RideRepository,
    settingsRepository: SettingsRepository,
    rideId: String,
) : ViewModel() {

    private val _detail = MutableStateFlow<RideDetail?>(null)
    val detail: StateFlow<RideDetail?> = _detail.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    init {
        viewModelScope.launch {
            _detail.value = rideRepository.getRideDetail(rideId)
        }
    }
}
