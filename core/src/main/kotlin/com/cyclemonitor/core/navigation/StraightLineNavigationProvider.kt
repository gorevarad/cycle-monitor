package com.cyclemonitor.core.navigation

import com.cyclemonitor.core.geo.GeoMath

/**
 * Fallback [NavigationProvider] that draws a direct line between origin and destination.
 * This is NOT real road/cycling-path routing -- it exists so navigation UI has something honest
 * to show (clearly marked [NavRoute.isApproximate] = true) when no real routing backend is
 * configured, rather than the app pretending to route or silently doing nothing. Replace with a
 * real cycling-routing backend (see [NavigationProvider] doc) before shipping turn-by-turn nav.
 */
class StraightLineNavigationProvider : NavigationProvider {
    override suspend fun calculateRoute(request: RouteRequest): Result<NavRoute> {
        val distance = GeoMath.haversineMeters(
            request.origin.latitude,
            request.origin.longitude,
            request.destination.latitude,
            request.destination.longitude,
        )
        if (distance <= 0.0) {
            return Result.failure(IllegalArgumentException("Origin and destination are the same point"))
        }
        return Result.success(
            NavRoute(
                points = listOf(request.origin, request.destination),
                distanceMeters = distance,
                estimatedDurationSeconds = null,
                steps = listOf(
                    NavStep(instruction = "Head toward destination (direct line, not a routed path)", distanceMeters = distance),
                ),
                isApproximate = true,
            ),
        )
    }
}
