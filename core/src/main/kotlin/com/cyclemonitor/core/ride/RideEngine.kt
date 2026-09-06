package com.cyclemonitor.core.ride

import com.cyclemonitor.core.geo.GeoMath
import com.cyclemonitor.core.model.GpsQuality
import com.cyclemonitor.core.model.LocationSample
import com.cyclemonitor.core.model.RiderProfile
import com.cyclemonitor.core.model.TrackPoint
import com.cyclemonitor.core.power.EstimationConfidence
import com.cyclemonitor.core.power.PowerEstimate
import com.cyclemonitor.core.power.PowerEstimationInput
import com.cyclemonitor.core.power.PowerEstimator
import com.cyclemonitor.core.smoothing.CumulativeAverage

/** Live, continuously-updated stats for the currently recording ride. */
data class RideLiveStats(
    /** Timestamp of the sample that produced this snapshot -- lets display-layer smoothers use
     * real elapsed time rather than wall-clock-at-processing. */
    val timestampMillis: Long,
    val distanceMeters: Double,
    val movingTimeSeconds: Long,
    val totalTimeSeconds: Long,
    val currentSpeedMps: Double,
    val averageSpeedMps: Double?,
    val maxSpeedMps: Double?,
    /** Null only if no altitude data has been available at all this ride. */
    val elevationGainMeters: Double?,
    val currentGradePercent: Double?,
    val currentPower: PowerEstimate,
    val averagePowerWatts: Double?,
    val maxPowerWatts: Double?,
    val gpsQuality: GpsQuality,
    val gpsAccuracyMeters: Float?,
)

/**
 * Pure ride-recording logic: ingests one [LocationSample] at a time and produces a [TrackPoint]
 * plus updated [RideLiveStats]. Contains no Android dependency (no Service, no database) so it
 * can run identically whether fed by real GPS or [com.cyclemonitor.core.location.MockLocationProvider],
 * and can be fully unit tested. The hosting recording service is responsible for persistence and
 * for surviving process death; this class only holds in-memory running totals for the active ride.
 */
class RideEngine(
    private val riderProfile: RiderProfile,
    private val powerEstimator: PowerEstimator,
    private val movingSpeedThresholdMps: Double = 0.8,
    private val elevationNoiseThresholdMeters: Double = 1.5,
    private val maxPlausibleSpeedMps: Double = 40.0,
) {
    data class StepResult(val trackPoint: TrackPoint, val liveStats: RideLiveStats)

    private var sequence = 0
    private var lastSample: LocationSample? = null
    private var lastSpeedMps: Double = 0.0
    private var isPaused = false

    private var distanceMeters = 0.0
    private var movingTimeSeconds = 0L
    private var totalTimeSeconds = 0L
    private var maxSpeedMps: Double? = null
    private var maxGradePercent: Double? = null

    private var elevationGainMeters: Double? = null
    private var elevationReferenceAltitude: Double? = null
    private var hasSeenAnyAltitude = false

    private val averageSpeed = CumulativeAverage()
    private val averagePower = CumulativeAverage()
    private var maxPowerWatts: Double? = null

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }

    /** Returns null if the sample is rejected outright (invalid coordinates). */
    fun onLocationSample(sample: LocationSample): StepResult? {
        if (!GeoMath.isValidCoordinate(sample.latitude, sample.longitude)) return null

        val previous = lastSample
        val dtSeconds = previous?.let { (sample.timestampMillis - it.timestampMillis) / 1000.0 }
        val distanceDeltaMeters = previous?.let {
            GeoMath.haversineMeters(it.latitude, it.longitude, sample.latitude, sample.longitude)
        } ?: 0.0

        val derivedSpeedMps = if (dtSeconds != null && dtSeconds > 0.0) distanceDeltaMeters / dtSeconds else 0.0
        val speedMps = sample.reportedSpeedMps
            ?.toDouble()
            ?.takeIf { it in 0.0..maxPlausibleSpeedMps }
            ?: derivedSpeedMps.coerceIn(0.0, maxPlausibleSpeedMps)

        val gradePercent = if (sample.altitudeMeters != null && previous?.altitudeMeters != null) {
            GeoMath.gradePercent(sample.altitudeMeters - previous.altitudeMeters, distanceDeltaMeters)
        } else {
            null
        }

        updateElevationGain(sample.altitudeMeters)

        val powerEstimate = powerEstimator.estimate(
            PowerEstimationInput(
                speedMps = speedMps,
                previousSpeedMps = if (previous != null) lastSpeedMps else null,
                dtSeconds = dtSeconds,
                gradePercent = gradePercent,
                gpsAccuracyMeters = sample.horizontalAccuracyMeters,
                riderProfile = riderProfile,
            ),
        )

        if (!isPaused) {
            distanceMeters += distanceDeltaMeters
            if (dtSeconds != null && dtSeconds > 0.0) {
                totalTimeSeconds += dtSeconds.toLong()
                if (speedMps > movingSpeedThresholdMps) {
                    movingTimeSeconds += dtSeconds.toLong()
                }
            }
            if (speedMps > movingSpeedThresholdMps) {
                averageSpeed.add(speedMps)
                maxSpeedMps = maxOf(maxSpeedMps ?: 0.0, speedMps)
            }
            gradePercent?.let { maxGradePercent = maxOf(maxGradePercent ?: it, it) }
            powerEstimate.rawWatts?.let { watts ->
                if (powerEstimate.confidence != EstimationConfidence.UNAVAILABLE) {
                    averagePower.add(watts)
                    maxPowerWatts = maxOf(maxPowerWatts ?: watts, watts)
                }
            }
        }

        val trackPoint = TrackPoint(
            sequence = sequence++,
            sample = sample,
            cumulativeDistanceMeters = distanceMeters,
            speedMps = speedMps,
            gradePercent = gradePercent,
            estimatedPowerWatts = powerEstimate.rawWatts,
            powerConfidence = powerEstimate.confidence,
            isPaused = isPaused,
        )

        val liveStats = RideLiveStats(
            timestampMillis = sample.timestampMillis,
            distanceMeters = distanceMeters,
            movingTimeSeconds = movingTimeSeconds,
            totalTimeSeconds = totalTimeSeconds,
            currentSpeedMps = speedMps,
            averageSpeedMps = averageSpeed.average,
            maxSpeedMps = maxSpeedMps,
            elevationGainMeters = elevationGainMeters,
            currentGradePercent = gradePercent,
            currentPower = powerEstimate,
            averagePowerWatts = averagePower.average,
            maxPowerWatts = maxPowerWatts,
            gpsQuality = GpsQuality.classify(sample.horizontalAccuracyMeters),
            gpsAccuracyMeters = sample.horizontalAccuracyMeters,
        )

        lastSample = sample
        lastSpeedMps = speedMps

        return StepResult(trackPoint, liveStats)
    }

    private fun updateElevationGain(altitudeMeters: Double?) {
        if (altitudeMeters == null) return
        hasSeenAnyAltitude = true
        if (elevationGainMeters == null) elevationGainMeters = 0.0

        val reference = elevationReferenceAltitude
        if (reference == null) {
            elevationReferenceAltitude = altitudeMeters
            return
        }
        val delta = altitudeMeters - reference
        when {
            delta > elevationNoiseThresholdMeters -> {
                elevationGainMeters = (elevationGainMeters ?: 0.0) + delta
                elevationReferenceAltitude = altitudeMeters
            }
            delta < -elevationNoiseThresholdMeters -> {
                elevationReferenceAltitude = altitudeMeters
            }
            // else: within the noise band, keep the old reference so small jitter doesn't drift it.
        }
    }
}
