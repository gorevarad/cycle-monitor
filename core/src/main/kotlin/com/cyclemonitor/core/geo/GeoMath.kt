package com.cyclemonitor.core.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure geographic math with no Android dependency, so it can be unit tested on a plain JVM.
 */
object GeoMath {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** True if both coordinates are within the valid WGS84 range and not the (0,0) "null island" sentinel. */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (latitude.isNaN() || longitude.isNaN()) return false
        if (latitude < -90.0 || latitude > 90.0) return false
        if (longitude < -180.0 || longitude > 180.0) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return true
    }

    /** Great-circle distance between two points, in meters. Returns 0.0 for identical/invalid points. */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) return 0.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /** Initial compass bearing from point 1 to point 2, in degrees [0, 360). */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    /**
     * Grade (slope) as a percentage: rise over run * 100.
     * Returns null when horizontal distance is too small to produce a meaningful grade
     * (avoids division-by-near-zero producing absurd spikes from GPS jitter).
     */
    fun gradePercent(elevationDeltaMeters: Double, horizontalDistanceMeters: Double): Double? {
        if (horizontalDistanceMeters < 3.0) return null
        return (elevationDeltaMeters / horizontalDistanceMeters) * 100.0
    }
}
