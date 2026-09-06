package com.cyclemonitor.core.ride

import com.cyclemonitor.core.model.LocationSample
import com.cyclemonitor.core.model.RiderProfile
import com.cyclemonitor.core.power.PowerEstimationEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RideEngineTest {

    private fun sample(
        t: Long,
        lat: Double,
        lon: Double,
        speed: Float? = null,
        accuracy: Float? = 4f,
        altitude: Double? = null,
    ) = LocationSample(
        timestampMillis = t,
        latitude = lat,
        longitude = lon,
        horizontalAccuracyMeters = accuracy,
        reportedSpeedMps = speed,
        speedAccuracyMps = null,
        bearingDegrees = null,
        altitudeMeters = altitude,
        altitudeAccuracyMeters = null,
    )

    private fun newEngine() = RideEngine(RiderProfile(), PowerEstimationEngine())

    @Test
    fun `first sample establishes baseline with zero distance`() {
        val engine = newEngine()
        val result = engine.onLocationSample(sample(0L, 37.0, -122.0))
        assertThat(result).isNotNull()
        assertThat(result!!.liveStats.distanceMeters).isEqualTo(0.0)
    }

    @Test
    fun `distance accumulates across consecutive samples`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 0.0001, 0.0, speed = 5f))
        val result = engine.onLocationSample(sample(1000L, 0.0002, 0.0, speed = 5f))
        assertThat(result!!.liveStats.distanceMeters).isGreaterThan(0.0)
    }

    @Test
    fun `invalid coordinates are rejected rather than corrupting the ride`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0))
        val rejected = engine.onLocationSample(sample(1000L, 0.0, 0.0))
        assertThat(rejected).isNull()
    }

    @Test
    fun `gps gap does not crash and still produces a track point`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, speed = 5f))
        // 30 second gap, simulating a tunnel or lost fix.
        val result = engine.onLocationSample(sample(30_000L, 37.001, -122.001, speed = 5f))
        assertThat(result).isNotNull()
    }

    @Test
    fun `gps jump does not produce an absurd speed`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0))
        // A huge coordinate jump in one second (GPS glitch) without a reported speed.
        val result = engine.onLocationSample(sample(1000L, 38.0, -121.0))
        assertThat(result!!.trackPoint.speedMps).isAtMost(40.0)
    }

    @Test
    fun `zero speed does not advance moving time`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, speed = 0f))
        val result = engine.onLocationSample(sample(1000L, 37.0, -122.0, speed = 0f))
        assertThat(result!!.liveStats.movingTimeSeconds).isEqualTo(0L)
    }

    @Test
    fun `paused samples do not add distance or time`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 0.0, 0.001, speed = 5f))
        engine.pause()
        val paused = engine.onLocationSample(sample(1000L, 0.0, 0.002, speed = 5f))
        assertThat(paused!!.trackPoint.isPaused).isTrue()
        val distanceWhilePaused = paused.liveStats.distanceMeters

        engine.resume()
        val resumed = engine.onLocationSample(sample(2000L, 0.0, 0.003, speed = 5f))
        assertThat(resumed!!.liveStats.distanceMeters).isGreaterThan(distanceWhilePaused)
    }

    @Test
    fun `missing elevation across the whole ride leaves elevation gain null`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, altitude = null))
        val result = engine.onLocationSample(sample(1000L, 37.0001, -122.0, altitude = null))
        assertThat(result!!.liveStats.elevationGainMeters).isNull()
    }

    @Test
    fun `steady climb accumulates elevation gain`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, altitude = 100.0))
        engine.onLocationSample(sample(1000L, 37.001, -122.0, altitude = 105.0))
        val result = engine.onLocationSample(sample(2000L, 37.002, -122.0, altitude = 110.0))
        assertThat(result!!.liveStats.elevationGainMeters).isGreaterThan(0.0)
    }

    @Test
    fun `small altitude jitter below the noise threshold is not counted as gain`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, altitude = 100.0))
        // 0.5m jitter is below the 1.5m noise threshold.
        val result = engine.onLocationSample(sample(1000L, 37.0001, -122.0, altitude = 100.5))
        assertThat(result!!.liveStats.elevationGainMeters).isEqualTo(0.0)
    }

    @Test
    fun `descending does not add to cumulative elevation gain`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, altitude = 100.0))
        val result = engine.onLocationSample(sample(1000L, 37.001, -122.0, altitude = 90.0))
        assertThat(result!!.liveStats.elevationGainMeters).isEqualTo(0.0)
    }

    @Test
    fun `very high but plausible speed is preserved not zeroed`() {
        val engine = newEngine()
        engine.onLocationSample(sample(0L, 37.0, -122.0, speed = 20f))
        val result = engine.onLocationSample(sample(1000L, 37.0002, -122.0, speed = 20f))
        assertThat(result!!.trackPoint.speedMps).isWithin(0.01).of(20.0)
    }

    @Test
    fun `sequence numbers increment even across rejected samples`() {
        val engine = newEngine()
        val first = engine.onLocationSample(sample(0L, 37.0, -122.0))
        engine.onLocationSample(sample(500L, 0.0, 0.0)) // rejected
        val third = engine.onLocationSample(sample(1000L, 37.0001, -122.0))
        assertThat(first!!.trackPoint.sequence).isEqualTo(0)
        assertThat(third!!.trackPoint.sequence).isEqualTo(1)
    }
}
