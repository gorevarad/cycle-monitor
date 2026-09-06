package com.cyclemonitor.core.stats

import com.cyclemonitor.core.model.RideSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

private fun ride(
    id: String,
    startTimeMillis: Long,
    distanceMeters: Double = 10_000.0,
    movingTimeSeconds: Long = 1800,
    elevationGainMeters: Double? = 100.0,
) = RideSummary(
    id = id,
    name = "Ride $id",
    startTimeMillis = startTimeMillis,
    endTimeMillis = startTimeMillis + movingTimeSeconds * 1000,
    distanceMeters = distanceMeters,
    movingTimeSeconds = movingTimeSeconds,
    totalTimeSeconds = movingTimeSeconds,
    averageSpeedMps = if (movingTimeSeconds > 0) distanceMeters / movingTimeSeconds else null,
    maxSpeedMps = 15.0,
    elevationGainMeters = elevationGainMeters,
    maxElevationMeters = 500.0,
    averageGradePercent = 1.0,
    maxGradePercent = 8.0,
    estimatedAveragePowerWatts = 180.0,
    estimatedMaxPowerWatts = 420.0,
)

class RideStatisticsAggregatorTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun `empty ride list produces empty statistics`() {
        val result = RideStatisticsAggregator.aggregate(emptyList(), null, Instant.now().toEpochMilli())
        assertThat(result).isEqualTo(PeriodStatistics.EMPTY)
    }

    @Test
    fun `rides outside the window are excluded`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val weekStart = RideStatisticsAggregator.startOfWeek(now, zone)
        val rides = listOf(
            ride("in-window", startTimeMillis = now.toEpochMilli()),
            ride("before-window", startTimeMillis = weekStart.minusSeconds(3600).toEpochMilli()),
        )
        val result = RideStatisticsAggregator.aggregate(rides, weekStart.toEpochMilli(), now.plusSeconds(1).toEpochMilli())
        assertThat(result.rideCount).isEqualTo(1)
    }

    @Test
    fun `distance and moving time sum across included rides`() {
        val now = Instant.now().toEpochMilli()
        val rides = listOf(
            ride("a", startTimeMillis = now - 1000, distanceMeters = 10_000.0, movingTimeSeconds = 1800),
            ride("b", startTimeMillis = now - 2000, distanceMeters = 20_000.0, movingTimeSeconds = 3600),
        )
        val result = RideStatisticsAggregator.aggregate(rides, null, now + 1)
        assertThat(result.distanceMeters).isEqualTo(30_000.0)
        assertThat(result.movingTimeSeconds).isEqualTo(5400L)
        assertThat(result.rideCount).isEqualTo(2)
    }

    @Test
    fun `average speed is total distance over total moving time not average of averages`() {
        val now = Instant.now().toEpochMilli()
        // Ride A: 10km in 1000s = 10 m/s. Ride B: 20km in 4000s = 5 m/s.
        // Naive average-of-averages would give 7.5 m/s; weighted gives 30000/5000 = 6 m/s.
        val rides = listOf(
            ride("a", startTimeMillis = now - 1000, distanceMeters = 10_000.0, movingTimeSeconds = 1000),
            ride("b", startTimeMillis = now - 2000, distanceMeters = 20_000.0, movingTimeSeconds = 4000),
        )
        val result = RideStatisticsAggregator.aggregate(rides, null, now + 1)
        assertThat(result.averageSpeedMps).isWithin(0.001).of(6.0)
    }

    @Test
    fun `rides with null elevation do not poison the total when others have data`() {
        val now = Instant.now().toEpochMilli()
        val rides = listOf(
            ride("has-elevation", startTimeMillis = now - 1000, elevationGainMeters = 200.0),
            ride("no-elevation", startTimeMillis = now - 2000, elevationGainMeters = null),
        )
        val result = RideStatisticsAggregator.aggregate(rides, null, now + 1)
        assertThat(result.elevationGainMeters).isEqualTo(200.0)
    }

    @Test
    fun `all rides missing elevation yields null not zero`() {
        val now = Instant.now().toEpochMilli()
        val rides = listOf(ride("no-elevation", startTimeMillis = now - 1000, elevationGainMeters = null))
        val result = RideStatisticsAggregator.aggregate(rides, null, now + 1)
        assertThat(result.elevationGainMeters).isNull()
    }

    @Test
    fun `invalid ride with zero distance and time is excluded from statistics`() {
        val now = Instant.now().toEpochMilli()
        val rides = listOf(ride("empty", startTimeMillis = now - 1000, distanceMeters = 0.0, movingTimeSeconds = 0))
        val result = RideStatisticsAggregator.aggregate(rides, null, now + 1)
        assertThat(result).isEqualTo(PeriodStatistics.EMPTY)
    }

    @Test
    fun `start of week is a monday`() {
        val wednesday = Instant.parse("2026-06-17T15:00:00Z") // a Wednesday
        val start = RideStatisticsAggregator.startOfWeek(wednesday, zone)
        assertThat(start.atZone(zone).dayOfWeek.value).isEqualTo(1) // Monday
        assertThat(start.isBefore(wednesday)).isTrue()
    }

    @Test
    fun `start of month is the first day`() {
        val midMonth = Instant.parse("2026-06-17T15:00:00Z")
        val start = RideStatisticsAggregator.startOfMonth(midMonth, zone)
        assertThat(start.atZone(zone).dayOfMonth).isEqualTo(1)
    }

    @Test
    fun `start of year is january first`() {
        val midYear = Instant.parse("2026-06-17T15:00:00Z")
        val start = RideStatisticsAggregator.startOfYear(midYear, zone)
        assertThat(start.atZone(zone).dayOfYear).isEqualTo(1)
    }
}
