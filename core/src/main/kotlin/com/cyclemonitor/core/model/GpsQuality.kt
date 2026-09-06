package com.cyclemonitor.core.model

/**
 * Qualitative bucket for the GPS fix quality, derived only from what the platform location API
 * actually reports (horizontal accuracy radius, in meters). Never fabricated.
 */
enum class GpsQuality {
    UNAVAILABLE,
    WEAK,
    MODERATE,
    GOOD;

    companion object {
        /**
         * Classifies fix quality from the reported horizontal accuracy (meters, 68% confidence
         * radius as returned by Android's Location#getAccuracy()).
         *
         * Thresholds are a reasonable, documented default for cycling use (not a vendor spec):
         *  - GOOD:      <= 8 m
         *  - MODERATE:  <= 20 m
         *  - WEAK:      > 20 m
         *  - UNAVAILABLE: accuracy missing entirely (no fix / permission denied / provider off)
         */
        fun classify(accuracyMeters: Float?): GpsQuality = when {
            accuracyMeters == null || accuracyMeters.isNaN() -> UNAVAILABLE
            accuracyMeters <= 8f -> GOOD
            accuracyMeters <= 20f -> MODERATE
            else -> WEAK
        }
    }
}
