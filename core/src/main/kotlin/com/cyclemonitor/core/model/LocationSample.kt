package com.cyclemonitor.core.model

/**
 * A single, untouched reading from the platform location provider (or a mock provider in
 * development builds). This is the raw sensor truth: nothing here is smoothed, interpolated,
 * or fabricated. Recording pipelines must persist this shape (or a lossless mapping of it)
 * so a ride can be fully reconstructed later.
 */
data class LocationSample(
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    /** Horizontal accuracy radius in meters, as reported by the platform. Null if unknown. */
    val horizontalAccuracyMeters: Float?,
    /** Speed in m/s as reported directly by the location provider, if it reports one. Null if unknown. */
    val reportedSpeedMps: Float?,
    /** Confidence for [reportedSpeedMps], as reported by the platform (Android 12+). Null if unknown. */
    val speedAccuracyMps: Float?,
    val bearingDegrees: Float?,
    val altitudeMeters: Double?,
    val altitudeAccuracyMeters: Float?,
    /** True if this sample came from a mock/simulated provider. Never true on a production build. */
    val isMocked: Boolean = false,
)
