package com.cyclemonitor.app.ride.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.repository.DashboardProfileRepository
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.app.map.RiderPosition
import com.cyclemonitor.app.ride.service.RideRecordingService
import com.cyclemonitor.core.model.DashboardProfile
import com.cyclemonitor.core.model.GpsQuality
import com.cyclemonitor.core.power.EstimationConfidence
import com.cyclemonitor.core.ride.RideLiveStats
import com.cyclemonitor.core.ride.RideState
import com.cyclemonitor.core.smoothing.DisplaySmoother
import com.cyclemonitor.core.smoothing.TimeWindowAverage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val FALLBACK_PROFILE = DashboardProfile.defaultRoadProfile("road", 0xFF39D6E0.toInt())

class RideViewModel(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val dashboardProfileRepository: DashboardProfileRepository,
) : ViewModel() {

    // Smoothing state is owned here (display layer), not in the recording service: the service's
    // recorded track points always keep the raw, unsmoothed estimate.
    private val speedSmoother = DisplaySmoother(timeConstantSeconds = 2.0)
    private val powerSmoother = DisplaySmoother(timeConstantSeconds = 3.0)
    private val power3sWindow = TimeWindowAverage(windowSeconds = 3.0)
    private var lastStatsTimestamp: Long? = null

    private val activeDashboardProfile: StateFlow<DashboardProfile> = combine(
        dashboardProfileRepository.observeProfiles(),
        settingsRepository.settings,
    ) { profiles, settings -> profiles.find { it.id == settings.dashboardProfileId } ?: FALLBACK_PROFILE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FALLBACK_PROFILE)

    val uiState: StateFlow<RideUiState> = combine(
        RideRecordingService.rideState,
        RideRecordingService.liveStats,
        settingsRepository.settings,
        activeDashboardProfile,
    ) { state, stats, settings, profile -> buildUiState(state, stats, settings, profile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RideUiState.initial())

    val riderPosition: StateFlow<RiderPosition?> = RideRecordingService.currentSample
        .map { sample -> sample?.let { RiderPosition(it.latitude, it.longitude, it.bearingDegrees) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Snapshot of the last non-idle [uiState] captured while FINISHING/COMPLETED, since stats
     * reset to null once the service returns to IDLE -- see RideTransitionOverlays.kt. */
    private val _finishSummary = MutableStateFlow<RideUiState?>(null)
    val finishSummary: StateFlow<RideUiState?> = _finishSummary.asStateFlow()

    val lastCompletedRideId: StateFlow<String?> = RideRecordingService.lastCompletedRideId

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.rideState is RideState.Finishing || state.rideState is RideState.Completed) {
                    _finishSummary.value = state
                }
            }
        }
    }

    fun acknowledgeRideFinished() {
        _finishSummary.value = null
        RideRecordingService.lastCompletedRideId.value = null
    }

    fun onStartRide() = RideRecordingService.start(appContext)
    fun onPauseRide() = RideRecordingService.pause(appContext)
    fun onResumeRide() = RideRecordingService.resume(appContext)
    fun onFinishRide() = RideRecordingService.finish(appContext)

    private fun buildUiState(state: RideState, stats: RideLiveStats?, settings: UserSettings, profile: DashboardProfile): RideUiState {
        if (stats == null || state == RideState.Idle) {
            speedSmoother.reset()
            powerSmoother.reset()
            power3sWindow.reset()
            lastStatsTimestamp = null
        }

        val displaySpeedMps = stats?.let {
            // Guard against a duplicate/out-of-order timestamp feeding the smoother a zero/negative dt.
            if (lastStatsTimestamp == it.timestampMillis) speedSmoother.currentValue
            else speedSmoother.update(it.timestampMillis, it.currentSpeedMps)
        }
        val displayPowerRaw = stats?.currentPower?.rawWatts?.let {
            if (lastStatsTimestamp == stats.timestampMillis) powerSmoother.currentValue
            else powerSmoother.update(stats.timestampMillis, it)
        }
        val power3sAvg = stats?.currentPower?.rawWatts?.let {
            if (lastStatsTimestamp != stats.timestampMillis) power3sWindow.add(stats.timestampMillis, it) else power3sWindow.average
        }
        lastStatsTimestamp = stats?.timestampMillis

        return RideUiState(
            rideState = state,
            speedUnit = settings.speedUnit,
            distanceUnit = settings.distanceUnit,
            elevationUnit = settings.elevationUnit,
            displaySpeed = displaySpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) },
            averageSpeed = stats?.averageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) },
            maxSpeed = stats?.maxSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) },
            distance = settings.distanceUnit.fromMeters(stats?.distanceMeters ?: 0.0),
            elapsedTimeSeconds = stats?.totalTimeSeconds ?: 0L,
            movingTimeSeconds = stats?.movingTimeSeconds ?: 0L,
            elevationGain = stats?.elevationGainMeters?.let { settings.elevationUnit.fromMeters(it) },
            gradePercent = stats?.currentGradePercent,
            displayPowerWatts = displayPowerRaw,
            power3sAvgWatts = power3sAvg,
            averagePowerWatts = stats?.averagePowerWatts,
            maxPowerWatts = stats?.maxPowerWatts,
            powerConfidence = stats?.currentPower?.confidence ?: EstimationConfidence.UNAVAILABLE,
            gpsQuality = stats?.gpsQuality ?: GpsQuality.UNAVAILABLE,
            gpsAccuracyMeters = stats?.gpsAccuracyMeters,
            visibleMetrics = profile.metrics,
            speedGaugeMaxKmh = profile.speedGaugeMaxKmh,
            powerGaugeMaxWatts = profile.powerGaugeMaxWatts,
            animationIntensity = settings.animationIntensity,
            accentColorArgb = profile.accentColorArgb,
            mapStyle = settings.mapStyle,
        )
    }
}
