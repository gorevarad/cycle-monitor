package com.cyclemonitor.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.repository.DashboardProfileRepository
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.core.model.DashboardMetric
import com.cyclemonitor.core.model.DashboardProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardCustomizationViewModel(
    private val repository: DashboardProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val profiles: StateFlow<List<DashboardProfile>> = repository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedProfileId: StateFlow<String> = settingsRepository.settings
        .map { it.dashboardProfileId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "road")

    fun selectProfile(id: String) = viewModelScope.launch { settingsRepository.updateDashboardProfileId(id) }

    fun duplicateProfile(basedOn: DashboardProfile) = viewModelScope.launch {
        val copy = basedOn.copy(id = UUID.randomUUID().toString(), name = "${basedOn.name} copy")
        repository.createCustomProfile(copy)
        settingsRepository.updateDashboardProfileId(copy.id)
    }

    fun deleteProfile(id: String) = viewModelScope.launch {
        repository.deleteProfile(id)
        if (selectedProfileId.value == id) settingsRepository.updateDashboardProfileId("road")
    }

    fun toggleMetric(profile: DashboardProfile, metric: DashboardMetric) = viewModelScope.launch {
        val newMetrics = if (metric in profile.metrics) profile.metrics - metric else profile.metrics + metric
        repository.updateProfile(profile.copy(metrics = newMetrics))
    }

    /** Moves [metric] one position earlier/later in the profile's display order. */
    fun reorderMetric(profile: DashboardProfile, metric: DashboardMetric, moveUp: Boolean) = viewModelScope.launch {
        val index = profile.metrics.indexOf(metric)
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (index < 0 || targetIndex < 0 || targetIndex >= profile.metrics.size) return@launch
        val newMetrics = profile.metrics.toMutableList().apply {
            val item = removeAt(index)
            add(targetIndex, item)
        }
        repository.updateProfile(profile.copy(metrics = newMetrics))
    }

    fun setSpeedGaugeMax(profile: DashboardProfile, maxKmh: Int) = viewModelScope.launch {
        repository.updateProfile(profile.copy(speedGaugeMaxKmh = maxKmh))
    }

    fun setPowerGaugeMax(profile: DashboardProfile, maxWatts: Int) = viewModelScope.launch {
        repository.updateProfile(profile.copy(powerGaugeMaxWatts = maxWatts))
    }

    fun setAccentColor(profile: DashboardProfile, colorArgb: Int) = viewModelScope.launch {
        repository.updateProfile(profile.copy(accentColorArgb = colorArgb))
    }

    fun renameProfile(profile: DashboardProfile, newName: String) = viewModelScope.launch {
        repository.updateProfile(profile.copy(name = newName))
    }
}
