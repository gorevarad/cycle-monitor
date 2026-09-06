package com.cyclemonitor.app.ride.ui

import com.cyclemonitor.app.data.settings.MapStyle
import com.cyclemonitor.core.model.AnimationIntensity
import com.cyclemonitor.core.model.DashboardMetric
import com.cyclemonitor.core.model.GpsQuality
import com.cyclemonitor.core.power.EstimationConfidence
import com.cyclemonitor.core.ride.RideState
import com.cyclemonitor.core.units.DistanceUnit
import com.cyclemonitor.core.units.ElevationUnit
import com.cyclemonitor.core.units.SpeedUnit

/**
 * Everything the ride dashboard needs to render one frame. All numeric fields are display-ready
 * (already unit-converted and, where relevant, smoothed) -- the recorded raw data lives only in
 * the recording service/database, never here.
 */
data class RideUiState(
    val rideState: RideState,
    val speedUnit: SpeedUnit,
    val distanceUnit: DistanceUnit,
    val elevationUnit: ElevationUnit,
    /** Null before the first GPS fix arrives -- render "--", never 0. */
    val displaySpeed: Double?,
    val averageSpeed: Double?,
    val maxSpeed: Double?,
    val distance: Double,
    val elapsedTimeSeconds: Long,
    val movingTimeSeconds: Long,
    val elevationGain: Double?,
    val gradePercent: Double?,
    val displayPowerWatts: Double?,
    val power3sAvgWatts: Double?,
    val averagePowerWatts: Double?,
    val maxPowerWatts: Double?,
    val powerConfidence: EstimationConfidence,
    val gpsQuality: GpsQuality,
    val gpsAccuracyMeters: Float?,
    val visibleMetrics: List<DashboardMetric>,
    val speedGaugeMaxKmh: Int,
    val powerGaugeMaxWatts: Int,
    val animationIntensity: AnimationIntensity,
    val accentColorArgb: Int,
    val mapStyle: MapStyle,
) {
    val isRiding: Boolean get() = rideState is RideState.Riding
    val isPaused: Boolean get() = rideState is RideState.Paused
    val isActive: Boolean get() = isRiding || isPaused || rideState is RideState.Starting || rideState is RideState.Finishing

    companion object {
        fun initial() = RideUiState(
            rideState = RideState.Idle,
            speedUnit = SpeedUnit.KMH,
            distanceUnit = DistanceUnit.KILOMETERS,
            elevationUnit = ElevationUnit.METERS,
            displaySpeed = null,
            averageSpeed = null,
            maxSpeed = null,
            distance = 0.0,
            elapsedTimeSeconds = 0,
            movingTimeSeconds = 0,
            elevationGain = null,
            gradePercent = null,
            displayPowerWatts = null,
            power3sAvgWatts = null,
            averagePowerWatts = null,
            maxPowerWatts = null,
            powerConfidence = EstimationConfidence.UNAVAILABLE,
            gpsQuality = GpsQuality.UNAVAILABLE,
            gpsAccuracyMeters = null,
            visibleMetrics = emptyList(),
            speedGaugeMaxKmh = 60,
            powerGaugeMaxWatts = 400,
            animationIntensity = AnimationIntensity.STANDARD,
            accentColorArgb = 0xFF39D6E0.toInt(),
            mapStyle = MapStyle.STANDARD,
        )
    }
}
