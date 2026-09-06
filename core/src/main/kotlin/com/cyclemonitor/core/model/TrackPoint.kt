package com.cyclemonitor.core.model

import com.cyclemonitor.core.power.EstimationConfidence

/**
 * One recorded point of a ride: the raw [LocationSample] plus values derived at record time
 * (cumulative distance, grade, estimated power). Derived fields are computed once from real
 * inputs -- never fabricated -- and stored so a ride can be redrawn/re-analyzed without
 * re-running location hardware.
 */
data class TrackPoint(
    val sequence: Int,
    val sample: LocationSample,
    val cumulativeDistanceMeters: Double,
    /** Raw (unsmoothed) instantaneous speed used for recording/statistics, in m/s. */
    val speedMps: Double,
    val gradePercent: Double?,
    val estimatedPowerWatts: Double?,
    val powerConfidence: EstimationConfidence,
    val isPaused: Boolean,
)
