package com.cyclemonitor.core.units

/** Display unit for speed. Storage is always meters/second (SI); this only affects presentation. */
enum class SpeedUnit {
    KMH,
    MPH;

    fun fromMetersPerSecond(mps: Double): Double = when (this) {
        KMH -> mps * 3.6
        MPH -> mps * 2.2369362920544
    }

    fun symbol(): String = when (this) {
        KMH -> "km/h"
        MPH -> "mph"
    }
}

enum class DistanceUnit {
    KILOMETERS,
    MILES;

    fun fromMeters(meters: Double): Double = when (this) {
        KILOMETERS -> meters / 1000.0
        MILES -> meters / 1609.344
    }

    fun symbol(): String = when (this) {
        KILOMETERS -> "km"
        MILES -> "mi"
    }
}

enum class ElevationUnit {
    METERS,
    FEET;

    fun fromMeters(meters: Double): Double = when (this) {
        METERS -> meters
        FEET -> meters * 3.280839895
    }

    fun symbol(): String = when (this) {
        METERS -> "m"
        FEET -> "ft"
    }
}
