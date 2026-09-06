package com.cyclemonitor.app.ride.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.map.RiderPosition
import com.cyclemonitor.app.ride.service.RideRecordingService
import com.cyclemonitor.core.model.DashboardProfile
import com.cyclemonitor.core.ride.RideLiveStats
import com.cyclemonitor.core.ride.RideState
import com.cyclemonitor.core.smoothing.DisplaySmoother
import com.cyclemonitor.core.smoothing.TimeWindowAverage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RideViewModel(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Smoothing state is owned here (display layer), not in the recording service: the service's
    // recorded track points always keep the raw, unsmoothed estimate.
    private val speedSmoother = DisplaySmoother(timeConstantSeconds = 2.0)
    private val powerSmoother = DisplaySmoother(timeConstantSeconds = 3.0)
    private val power3sWindow = TimeWindowAverage(windowSeconds = 3.0)
    private var lastStatsTimestamp: Long? = null

    val uiState: StateFlow<RideUiState> = combine(
        RideRecordingService.rideState,
        RideRecordingService.liveStats,
        settingsRepository.settings,
    ) { state, stats, settings -> buildUiState(state, stats, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RideUiState.initial())

    val riderPosition: StateFlow<RiderPosition?> = RideRecordingService.currentSample
        .map { sample -> sample?.let { RiderPosition(it.latitude, it.longitude, it.bearingDegrees) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onStartRide() = RideRecordingService.start(appContext)
    fun onPauseRide() = RideRecordingService.pause(appContext)
    fun onResumeRide() = RideRecordingService.resume(appContext)
    fun onFinishRide() = RideRecordingService.finish(appContext)

    private fun buildUiState(state: RideState, stats: RideLiveStats?, settings: UserSettings): RideUiState {
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

        val profile = dashboardProfileFor(settings.dashboardProfileId)

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
            powerConfidence = stats?.currentPower?.confidence ?: com.cyclemonitor.core.power.EstimationConfidence.UNAVAILABLE,
            gpsQuality = stats?.gpsQuality ?: com.cyclemonitor.core.model.GpsQuality.UNAVAILABLE,
            gpsAccuracyMeters = stats?.gpsAccuracyMeters,
            visibleMetrics = profile.metrics,
            speedGaugeMaxKmh = profile.speedGaugeMaxKmh,
            powerGaugeMaxWatts = profile.powerGaugeMaxWatts,
            animationIntensity = settings.animationIntensity,
            accentColorArgb = profile.accentColorArgb,
        )
    }

    private fun dashboardProfileFor(id: String): DashboardProfile {
        val accent = 0xFF39D6E0.toInt()
        return when (id) {
            "climb" -> DashboardProfile.climbProfile(id, accent)
            "race" -> DashboardProfile.raceProfile(id, accent)
            "casual" -> DashboardProfile.casualProfile(id, accent)
            else -> DashboardProfile.defaultRoadProfile("road", accent)
        }
    }
}
