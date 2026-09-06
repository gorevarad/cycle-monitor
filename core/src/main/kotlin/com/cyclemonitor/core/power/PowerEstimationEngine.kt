package com.cyclemonitor.core.power

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * Physics-based propulsive power estimator. THE PHONE HAS NO POWER METER: this computes the
 * mechanical power that *would* be required to produce the observed speed/acceleration/grade
 * under a set of documented assumptions, then divides by drivetrain efficiency to approximate
 * rider pedal power. It is always an approximation and must be surfaced to the user as such.
 *
 * Model (forces along the direction of travel, summed then multiplied by ground speed):
 *  - Gravity:   F_g   = m * g * sin(atan(grade / 100))
 *  - Rolling:   F_rr  = Crr * m * g * cos(atan(grade / 100))
 *  - Aerodynamic: F_aero = 0.5 * rho * CdA * v_air^2   (v_air = ground speed + headwind)
 *  - Acceleration: F_a = m * dv/dt
 *  Rider power = max(0, (F_g + F_rr + F_aero + F_a) * v_ground) / drivetrainEfficiency
 *
 * Documented assumptions / known limitations:
 *  - Wheel/drivetrain rotational inertia is ignored (small effect versus rider+bike linear mass).
 *  - Headwind defaults to 0 (no headwind) when unknown -- the phone has no reliable wind sensor,
 *    so still-air aero drag is used unless a caller supplies a measured/forecast wind component.
 *  - Air density defaults to sea-level standard (1.225 kg/m^3) unless the caller supplies one
 *    derived from altitude/temperature.
 *  - When required mechanical power is <= 0 (freewheeling downhill, braking, decelerating), the
 *    model cannot distinguish "coasting" from "very light pedaling", so it reports 0 W with
 *    reduced confidence rather than a negative or fabricated positive number.
 *  - Extreme or implausible accelerations (GPS jumps) are clamped and flagged rather than
 *    allowed to produce wild power spikes.
 */
class PowerEstimationEngine : PowerEstimator {

    override fun estimate(input: PowerEstimationInput): PowerEstimate {
        val reasons = mutableSetOf<LowConfidenceReason>()

        val dt = input.dtSeconds
        if (dt == null || dt <= 0.0) {
            return PowerEstimate(rawWatts = null, confidence = EstimationConfidence.UNAVAILABLE, reasons = setOf(LowConfidenceReason.STALE_OR_MISSING_FIX))
        }
        if (dt > MAX_USABLE_GAP_SECONDS) {
            return PowerEstimate(rawWatts = null, confidence = EstimationConfidence.UNAVAILABLE, reasons = setOf(LowConfidenceReason.LARGE_TIME_GAP))
        }
        if (dt > LARGE_GAP_WARNING_SECONDS) {
            reasons += LowConfidenceReason.LARGE_TIME_GAP
        }

        val speed = input.speedMps.coerceAtLeast(0.0)

        if (input.gpsAccuracyMeters == null || input.gpsAccuracyMeters > WEAK_ACCURACY_METERS) {
            reasons += LowConfidenceReason.WEAK_GPS_ACCURACY
        }

        val gradePercent = input.gradePercent
        if (gradePercent == null) {
            reasons += LowConfidenceReason.MISSING_ELEVATION
        }
        val effectiveGradePercent = (gradePercent ?: 0.0).coerceIn(-30.0, 30.0)

        var acceleration = if (input.previousSpeedMps != null) (speed - input.previousSpeedMps) / dt else 0.0
        if (abs(acceleration) > PLAUSIBLE_MAX_ACCELERATION_MPS2) {
            reasons += LowConfidenceReason.GPS_SPEED_JITTER
            acceleration = acceleration.coerceIn(-PLAUSIBLE_MAX_ACCELERATION_MPS2, PLAUSIBLE_MAX_ACCELERATION_MPS2)
        }

        val profile = input.riderProfile
        val mass = profile.totalMassKg
        val theta = atan(effectiveGradePercent / 100.0)

        val gravityForce = mass * GRAVITY_MPS2 * sin(theta)
        val rollingForce = profile.bikeType.defaultCrr * mass * GRAVITY_MPS2 * cos(theta)
        val airSpeed = speed + (input.headwindMps ?: 0.0)
        val aeroForce = 0.5 * input.airDensityKgM3 * profile.effectiveCdA * airSpeed * abs(airSpeed)
        val accelerationForce = mass * acceleration

        val mechanicalPowerAtWheel = (gravityForce + rollingForce + aeroForce + accelerationForce) * speed

        val rawWatts: Double
        if (mechanicalPowerAtWheel <= 0.0) {
            rawWatts = 0.0
            if (speed > STOPPED_SPEED_THRESHOLD_MPS) {
                reasons += LowConfidenceReason.COASTING_OR_DESCENDING
            }
        } else {
            rawWatts = mechanicalPowerAtWheel / profile.drivetrainEfficiency
        }

        val confidence = when {
            speed <= STOPPED_SPEED_THRESHOLD_MPS -> EstimationConfidence.HIGH
            reasons.size >= 2 -> EstimationConfidence.LOW
            reasons.size == 1 -> EstimationConfidence.MEDIUM
            else -> EstimationConfidence.HIGH
        }

        return PowerEstimate(rawWatts = rawWatts, confidence = confidence, reasons = reasons)
    }

    private companion object {
        const val GRAVITY_MPS2 = 9.80665
        const val WEAK_ACCURACY_METERS = 20f
        const val PLAUSIBLE_MAX_ACCELERATION_MPS2 = 3.0
        const val STOPPED_SPEED_THRESHOLD_MPS = 0.3
        const val LARGE_GAP_WARNING_SECONDS = 5.0
        const val MAX_USABLE_GAP_SECONDS = 15.0
    }
}
