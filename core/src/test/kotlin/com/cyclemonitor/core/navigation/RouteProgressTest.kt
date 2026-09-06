package com.cyclemonitor.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RouteProgressTest {

    // Three points along a meridian so haversine distances are predictable: each 0.001 degree of
    // latitude is ~111.2 m.
    private val pointA = RoutePoint(10.000, 20.000)
    private val pointB = RoutePoint(10.001, 20.000)
    private val pointC = RoutePoint(10.002, 20.000)
    private val route = listOf(pointA, pointB, pointC)

    @Test
    fun `remaining distance is null for a route with fewer than two points`() {
        assertThat(RouteProgress.remainingDistanceMeters(listOf(pointA), 10.0, 20.0)).isNull()
        assertThat(RouteProgress.remainingDistanceMeters(emptyList(), 10.0, 20.0)).isNull()
    }

    @Test
    fun `remaining distance at the destination is approximately zero`() {
        val remaining = RouteProgress.remainingDistanceMeters(route, pointC.latitude, pointC.longitude)
        assertThat(remaining).isNotNull()
        assertThat(remaining!!).isWithin(1.0).of(0.0)
    }

    @Test
    fun `remaining distance at the start covers the whole route`() {
        val remaining = RouteProgress.remainingDistanceMeters(route, pointA.latitude, pointA.longitude)
        assertThat(remaining).isNotNull()
        // ~111.2m per leg, two legs.
        assertThat(remaining!!).isWithin(2.0).of(222.4)
    }

    @Test
    fun `remaining distance decreases monotonically as the rider advances vertex to vertex`() {
        val atStart = RouteProgress.remainingDistanceMeters(route, pointA.latitude, pointA.longitude)!!
        val atMiddle = RouteProgress.remainingDistanceMeters(route, pointB.latitude, pointB.longitude)!!
        val atEnd = RouteProgress.remainingDistanceMeters(route, pointC.latitude, pointC.longitude)!!
        assertThat(atStart).isGreaterThan(atMiddle)
        assertThat(atMiddle).isGreaterThan(atEnd)
    }

    @Test
    fun `distance from route is null for an empty route`() {
        assertThat(RouteProgress.distanceFromRouteMeters(emptyList(), 10.0, 20.0)).isNull()
    }

    @Test
    fun `distance from route is zero when exactly on a route point`() {
        val distance = RouteProgress.distanceFromRouteMeters(route, pointB.latitude, pointB.longitude)
        assertThat(distance).isNotNull()
        assertThat(distance!!).isWithin(0.5).of(0.0)
    }

    // The route runs along a meridian (fixed longitude), so offsetting purely in longitude at one
    // of the route's latitudes gives a clean, roughly-known perpendicular distance from it:
    // ~0.0003 deg longitude at 10 deg latitude is ~33m; ~0.002 deg is ~219m.
    private val nearbyOffRouteLongitude = pointB.longitude + 0.0003
    private val farOffRouteLongitude = pointB.longitude + 0.002

    @Test
    fun `rider close to the route is not off route`() {
        assertThat(RouteProgress.isOffRoute(route, pointB.latitude, nearbyOffRouteLongitude)).isFalse()
    }

    @Test
    fun `rider far from the route is off route`() {
        assertThat(RouteProgress.isOffRoute(route, pointB.latitude, farOffRouteLongitude)).isTrue()
    }

    @Test
    fun `off route threshold is configurable`() {
        // ~33m away: off-route with a tight 20m threshold, on-route with the 60m default.
        assertThat(RouteProgress.isOffRoute(route, pointB.latitude, nearbyOffRouteLongitude, thresholdMeters = 20.0)).isTrue()
        assertThat(RouteProgress.isOffRoute(route, pointB.latitude, nearbyOffRouteLongitude, thresholdMeters = 60.0)).isFalse()
    }

    private val steps = listOf(
        NavStep("Head north", distanceMeters = 100.0),
        NavStep("Turn right", distanceMeters = 200.0),
        NavStep("Arrive at destination", distanceMeters = 50.0),
    )

    @Test
    fun `current step index is null for an empty step list`() {
        assertThat(RouteProgress.currentStepIndex(emptyList(), 10.0)).isNull()
    }

    @Test
    fun `current step index starts at zero`() {
        assertThat(RouteProgress.currentStepIndex(steps, 0.0)).isEqualTo(0)
    }

    @Test
    fun `current step index advances once the first step's distance is covered`() {
        assertThat(RouteProgress.currentStepIndex(steps, 50.0)).isEqualTo(0)
        assertThat(RouteProgress.currentStepIndex(steps, 150.0)).isEqualTo(1)
        assertThat(RouteProgress.currentStepIndex(steps, 320.0)).isEqualTo(2)
    }

    @Test
    fun `current step index stays on the last step once travel exceeds the route`() {
        assertThat(RouteProgress.currentStepIndex(steps, 10_000.0)).isEqualTo(2)
    }
}
