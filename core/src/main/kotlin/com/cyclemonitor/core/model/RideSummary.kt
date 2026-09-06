package com.cyclemonitor.core.model

/**
 * Aggregate statistics for a completed ride. Every numeric field is nullable where the
 * underlying data may genuinely be unavailable (e.g. no elevation data on that device/run) --
 * the UI is responsible for rendering "N/A" rather than a fabricated zero.
 */
data class RideSummary(
    val id: String,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val distanceMeters: Double,
    val movingTimeSeconds: Long,
    val totalTimeSeconds: Long,
    val averageSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val elevationGainMeters: Double?,
    val maxElevationMeters: Double?,
    val averageGradePercent: Double?,
    val maxGradePercent: Double?,
    val estimatedAveragePowerWatts: Double?,
    val estimatedMaxPowerWatts: Double?,
) {
    /** A ride is only eligible for statistics/personal-records purposes if it has real motion data. */
    val isValidForStatistics: Boolean
        get() = distanceMeters > 0.0 && movingTimeSeconds > 0 && endTimeMillis > startTimeMillis
}

data class RideDetail(
    val summary: RideSummary,
    val trackPoints: List<TrackPoint>,
)
