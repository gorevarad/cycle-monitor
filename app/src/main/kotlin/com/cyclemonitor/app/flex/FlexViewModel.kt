package com.cyclemonitor.app.flex

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val uri: Uri) : ExportState()
    data class Failure(val message: String) : ExportState()
}

class FlexViewModel(
    private val rideRepository: RideRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val availableRides = rideRepository.observeRides()
        .map { rides -> rides.filter { it.isValidForStatistics } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _selectedRideId = MutableStateFlow<String?>(null)
    val selectedRideId: StateFlow<String?> = _selectedRideId.asStateFlow()

    private val _selectedRideDetail = MutableStateFlow<RideDetail?>(null)
    val selectedRideDetail: StateFlow<RideDetail?> = _selectedRideDetail.asStateFlow()

    private val _config = MutableStateFlow(FlexConfig())
    val config: StateFlow<FlexConfig> = _config.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun selectRide(rideId: String) {
        _selectedRideId.value = rideId
        viewModelScope.launch {
            _selectedRideDetail.value = rideRepository.getRideDetail(rideId)
        }
    }

    fun selectPreset(preset: FlexPreset) {
        _config.value = FlexConfig.forPreset(preset, _config.value)
    }

    fun updateConfig(transform: (FlexConfig) -> FlexConfig) {
        _config.value = transform(_config.value)
    }

    fun export(context: Context, widthPx: Int, heightPx: Int) {
        val detail = _selectedRideDetail.value ?: run {
            _exportState.value = ExportState.Failure("Select a ride first")
            return
        }
        _exportState.value = ExportState.Exporting
        viewModelScope.launch {
            val result = FlexPngExporter.export(
                context = context,
                config = _config.value,
                summary = detail.summary,
                routePoints = detail.trackPoints.map { it.sample.latitude to it.sample.longitude },
                settings = settings.value,
                widthPx = widthPx,
                heightPx = heightPx,
            )
            _exportState.value = result.fold(
                onSuccess = { ExportState.Success(it) },
                onFailure = { ExportState.Failure(it.message ?: "Export failed") },
            )
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}
