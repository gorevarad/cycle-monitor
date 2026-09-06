package com.cyclemonitor.core.navigation

/**
 * Route CALCULATION, deliberately separate from map DISPLAY (which lives in the `:app` UI layer
 * as a Google Maps Compose panel). A [NavigationProvider] turns an origin/destination into a
 * routable path; it does not render anything.
 *
 * IMPORTANT: the Google Maps Platform does not give an Android app on-device turn-by-turn
 * cycling directions "for free" -- a real implementation needs a routing backend (e.g. the
 * Google Routes API with a bicycling travel mode, or a third-party cycling router) that requires
 * its own API key/billing and must be verified against current Google Maps Platform docs before
 * shipping. Until that backend is wired up, [com.cyclemonitor.core.navigation.StraightLineNavigationProvider]
 * is the only implementation, and it must never be presented to the user as real turn-by-turn routing.
 */
interface NavigationProvider {
    suspend fun calculateRoute(request: RouteRequest): Result<NavRoute>
}

data class RoutePoint(val latitude: Double, val longitude: Double)

enum class RoutingProfile {
    CYCLING,
}

data class RouteRequest(
    val origin: RoutePoint,
    val destination: RoutePoint,
    val profile: RoutingProfile = RoutingProfile.CYCLING,
)

data class NavStep(
    val instruction: String,
    val distanceMeters: Double,
)

data class NavRoute(
    val points: List<RoutePoint>,
    val distanceMeters: Double,
    /** Null when no real routing backend is configured -- never fabricate an ETA. */
    val estimatedDurationSeconds: Long?,
    val steps: List<NavStep>,
    val isApproximate: Boolean,
)
