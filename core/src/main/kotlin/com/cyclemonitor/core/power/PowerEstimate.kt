package com.cyclemonitor.core.power

/**
 * How much the caller should trust a [PowerEstimate]. The UI must never render a confident,
 * precise-looking number when this is below HIGH — see the product rule that estimated power
 * is always labeled "EST." and degrades to "LOW CONFIDENCE" / unavailable rather than lying.
 */
enum class EstimationConfidence {
    /** Inputs look solid: recent GPS fix, good accuracy, plausible acceleration, known grade. */
    HIGH,

    /** At least one input is degraded (weak GPS, missing elevation, choppy update rate). */
    MEDIUM,

    /** Multiple inputs are degraded or the model's assumptions are being stretched (e.g. descent). */
    LOW,

    /** Cannot produce a meaningful estimate at all (no fix, stale data, or negative dt). */
    UNAVAILABLE,
}

enum class LowConfidenceReason {
    WEAK_GPS_ACCURACY,
    MISSING_ELEVATION,
    GPS_SPEED_JITTER,
    STALE_OR_MISSING_FIX,
    COASTING_OR_DESCENDING,
    LARGE_TIME_GAP,
}

/**
 * Result of one power estimation pass.
 *
 * [rawWatts] is the untouched model output for this instant and is what gets recorded to the
 * ride's data store. [displayWatts] is a smoothed value meant purely for the live gauge/number
 * and must be produced by a caller-owned [com.cyclemonitor.core.smoothing.DisplaySmoother],
 * not baked in here, so the smoothing window stays configurable and testable independently.
 */
data class PowerEstimate(
    val rawWatts: Double?,
    val confidence: EstimationConfidence,
    val reasons: Set<LowConfidenceReason> = emptySet(),
)
