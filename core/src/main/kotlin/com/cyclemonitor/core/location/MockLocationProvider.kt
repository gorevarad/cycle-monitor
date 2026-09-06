package com.cyclemonitor.core.location

import com.cyclemonitor.core.geo.GeoMath
import com.cyclemonitor.core.model.LocationSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * DEVELOPMENT/TEST ONLY. Generates a synthetic, clearly-labeled ride (every emitted
 * [LocationSample] has [LocationSample.isMocked] = true) so the dashboard, ride recording, and
 * history/statistics screens can be exercised without physically riding a bike.
 *
 * This class must never be wired into a release build's production location dependency graph --
 * the DI wiring in the app module only constructs it for debug/demo build variants.
 */
class MockLocationProvider(
    private val startLatitude: Double = 37.7749,
    private val startLongitude: Double = -122.4194,
    private val updateIntervalMillis: Long = 1000L,
    private val random: Random = Random(seed = 42),
) : LocationProvider {

    override fun availability(): Flow<LocationAvailability> = flow {
        emit(LocationAvailability.AVAILABLE)
    }

    override fun locationUpdates(): Flow<LocationSample> = flow {
        var bearing = 0.0
        var elapsedSeconds = 0.0
        var lat = startLatitude
        var lon = startLongitude

        while (true) {
            // A gently varying speed profile (8-12 m/s, ~29-43 km/h) with slow random drift so
            // the UI has something realistic-looking to animate, plus a slow bearing turn so the
            // mock route isn't a perfectly straight line.
            val baseSpeed = 9.5 + 2.5 * sin(elapsedSeconds / 45.0)
            val speed = (baseSpeed + random.nextDouble(-0.4, 0.4)).coerceAtLeast(0.0)
            bearing = (bearing + random.nextDouble(-3.0, 3.0) + 0.4) % 360.0

            val dtSeconds = updateIntervalMillis / 1000.0
            val distanceMeters = speed * dtSeconds
            val bearingRad = Math.toRadians(bearing)
            val metersPerDegreeLat = 111_320.0
            val metersPerDegreeLon = 111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.01)
            lat += (distanceMeters * cos(bearingRad)) / metersPerDegreeLat
            lon += (distanceMeters * sin(bearingRad)) / metersPerDegreeLon

            check(GeoMath.isValidCoordinate(lat, lon)) { "Mock provider produced an invalid coordinate" }

            val altitude = 20.0 + 15.0 * sin(elapsedSeconds / 120.0 * PI)

            emit(
                LocationSample(
                    timestampMillis = System.currentTimeMillis(),
                    latitude = lat,
                    longitude = lon,
                    horizontalAccuracyMeters = (4f + random.nextFloat() * 3f),
                    reportedSpeedMps = speed.toFloat(),
                    speedAccuracyMps = 0.3f,
                    bearingDegrees = bearing.toFloat(),
                    altitudeMeters = altitude,
                    altitudeAccuracyMeters = 3f,
                    isMocked = true,
                ),
            )

            elapsedSeconds += dtSeconds
            delay(updateIntervalMillis)
        }
    }
}
