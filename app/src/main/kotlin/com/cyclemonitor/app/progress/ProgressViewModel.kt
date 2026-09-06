package com.cyclemonitor.app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.core.repository.RideRepository
import com.cyclemonitor.core.stats.PeriodStatistics
import com.cyclemonitor.core.stats.RideStatisticsAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

enum class StatsFilter(val label: String) {
    LAST_7_DAYS("7 DAYS"),
    LAST_30_DAYS("30 DAYS"),
    LAST_YEAR("1 YEAR"),
    ALL_TIME("ALL TIME"),
}

class ProgressViewModel(
    rideRepository: RideRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(StatsFilter.LAST_7_DAYS)
    val selectedFilter: StateFlow<StatsFilter> = _selectedFilter.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val statistics: StateFlow<PeriodStatistics> = combine(
        rideRepository.observeRides(),
        _selectedFilter,
    ) { rides, filter ->
        val now = Instant.now()
        val start = when (filter) {
            StatsFilter.LAST_7_DAYS -> now.minusSeconds(7L * 24 * 3600)
            StatsFilter.LAST_30_DAYS -> now.minusSeconds(30L * 24 * 3600)
            StatsFilter.LAST_YEAR -> now.minusSeconds(365L * 24 * 3600)
            StatsFilter.ALL_TIME -> null
        }
        RideStatisticsAggregator.aggregate(rides, start?.toEpochMilli(), now.toEpochMilli() + 1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodStatistics.EMPTY)

    fun selectFilter(filter: StatsFilter) {
        _selectedFilter.value = filter
    }
}
