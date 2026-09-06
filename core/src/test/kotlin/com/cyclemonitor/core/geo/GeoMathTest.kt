package com.cyclemonitor.core.geo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeoMathTest {

    @Test
    fun `haversine distance between identical points is zero`() {
        assertThat(GeoMath.haversineMeters(37.0, -122.0, 37.0, -122.0)).isEqualTo(0.0)
    }

    @Test
    fun `haversine distance for one degree of latitude is about 111 km`() {
        // Deliberately not (0,0) -- that's the null-island sentinel GeoMath treats as invalid.
        val distance = GeoMath.haversineMeters(10.0, 10.0, 11.0, 10.0)
        assertThat(distance).isWithin(1000.0).of(111_195.0)
    }

    @Test
    fun `invalid coordinates produce zero distance instead of garbage`() {
        val distance = GeoMath.haversineMeters(200.0, 0.0, 1.0, 0.0)
        assertThat(distance).isEqualTo(0.0)
    }

    @Test
    fun `null island sentinel is treated as invalid`() {
        assertThat(GeoMath.isValidCoordinate(0.0, 0.0)).isFalse()
    }

    @Test
    fun `out of range latitude is invalid`() {
        assertThat(GeoMath.isValidCoordinate(95.0, 10.0)).isFalse()
        assertThat(GeoMath.isValidCoordinate(-95.0, 10.0)).isFalse()
    }

    @Test
    fun `out of range longitude is invalid`() {
        assertThat(GeoMath.isValidCoordinate(10.0, 190.0)).isFalse()
    }

    @Test
    fun `NaN coordinates are invalid`() {
        assertThat(GeoMath.isValidCoordinate(Double.NaN, 10.0)).isFalse()
    }

    @Test
    fun `grade percent for short horizontal distance is null to avoid GPS-noise spikes`() {
        assertThat(GeoMath.gradePercent(elevationDeltaMeters = 5.0, horizontalDistanceMeters = 1.0)).isNull()
    }

    @Test
    fun `grade percent computes rise over run`() {
        val grade = GeoMath.gradePercent(elevationDeltaMeters = 5.0, horizontalDistanceMeters = 100.0)
        assertThat(grade).isWithin(0.001).of(5.0)
    }

    @Test
    fun `bearing due east is 90 degrees`() {
        val bearing = GeoMath.bearingDegrees(0.0, 0.0, 0.0, 1.0)
        assertThat(bearing).isWithin(1.0).of(90.0)
    }
}
