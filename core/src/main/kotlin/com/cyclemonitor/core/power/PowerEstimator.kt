package com.cyclemonitor.core.power

import com.cyclemonitor.core.model.RiderProfile

/**
 * One instant's worth of inputs for a power estimate. All physical quantities are SI
 * (meters, seconds, m/s, m/s^2). Unknown optional inputs must be passed as null rather than a
 * guessed default of 0 -- the estimator itself decides how an unknown input affects confidence.
 */
data class PowerEstimationInput(
    val speedMps: Double,
    val previousSpeedMps: Double?,
    val dtSeconds: Double?,
    /** Road/trail grade in percent, e.g. 4.5 for a 4.5% climb. Null if elevation is unavailable. */
    val gradePercent: Double?,
    val gpsAccuracyMeters: Float?,
    /** Headwind component along the direction of travel, m/s, positive = headwind. Null if unknown. */
    val headwindMps: Double? = null,
    val airDensityKgM3: Double = 1.225,
    val riderProfile: RiderProfile,
)

/**
 * Independent, UI-agnostic power estimation module. Implementations must document every
 * physical assumption they bake in, since a phone has no power meter and this is always an
 * approximation of propulsive power, never a measurement.
 */
interface PowerEstimator {
    fun estimate(input: PowerEstimationInput): PowerEstimate
}
