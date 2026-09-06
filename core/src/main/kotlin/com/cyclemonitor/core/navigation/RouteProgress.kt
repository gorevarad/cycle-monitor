package com.cyclemonitor.core.navigation

import com.cyclemonitor.core.geo.GeoMath

/**
 * Tracks a rider's progress along an already-calculated [NavRoute]. Pure math, no Android/Compose
 * dependency, so it's directly unit-testable -- see RouteProgressTest.
 *
 * Uses nearest-vertex distance rather than a true perpendicular nearest-point-on-segment
 * projection: simpler, and accurate enough for the reasonably dense polylines the Directions API
 * (or the straight-line fallback) returns. Documented simplification, not an oversight.
 */
object RouteProgress {

    /**
     * Remaining distance (meters) from [currentLatitude]/[currentLongitude] to the end of
     * [routePoints], following the route rather than a straight line to the destination. Returns
     * null if the route has fewer than 2 points (nothing to measure progress along).
     */
    fun remainingDistanceMeters(routePoints: List<RoutePoint>, currentLatitude: Double, currentLongitude: Double): Double? {
        if (routePoints.size < 2) return null

        var nearestIndex = 0
        var nearestDistance = Double.MAX_VALUE
        routePoints.forEachIndexed { index, point ->
            val distance = GeoMath.haversineMeters(currentLatitude, currentLongitude, point.latitude, point.longitude)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestIndex = index
            }
        }

        var remaining = nearestDistance
        for (i in nearestIndex until routePoints.size - 1) {
            remaining += GeoMath.haversineMeters(
                routePoints[i].latitude, routePoints[i].longitude,
                routePoints[i + 1].latitude, routePoints[i + 1].longitude,
            )
        }
        return remaining
    }

    /** Distance (meters) from the current position to the nearest point on the route. */
    fun distanceFromRouteMeters(routePoints: List<RoutePoint>, currentLatitude: Double, currentLongitude: Double): Double? {
        if (routePoints.isEmpty()) return null
        return routePoints.minOf { GeoMath.haversineMeters(currentLatitude, currentLongitude, it.latitude, it.longitude) }
    }

    /** True once the rider has drifted more than [thresholdMeters] from the calculated route. */
    fun isOffRoute(routePoints: List<RoutePoint>, currentLatitude: Double, currentLongitude: Double, thresholdMeters: Double = 60.0): Boolean {
        val distance = distanceFromRouteMeters(routePoints, currentLatitude, currentLongitude) ?: return false
        return distance > thresholdMeters
    }

    /**
     * Which turn-by-turn [NavStep] the rider is currently on, given how far they've traveled
     * along the route so far. Returns null only when [steps] is empty. Once past the last step's
     * cumulative distance, stays pinned to the last step rather than throwing.
     */
    fun currentStepIndex(steps: List<NavStep>, distanceTraveledMeters: Double): Int? {
        if (steps.isEmpty()) return null
        var cumulative = 0.0
        for (index in steps.indices) {
            cumulative += steps[index].distanceMeters
            if (distanceTraveledMeters < cumulative) return index
        }
        return steps.lastIndex
    }
}
