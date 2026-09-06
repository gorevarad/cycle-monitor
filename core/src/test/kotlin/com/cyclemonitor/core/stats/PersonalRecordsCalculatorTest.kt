package com.cyclemonitor.core.stats

import com.cyclemonitor.core.model.RideSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private fun ride(
    id: String,
    distanceMeters: Double,
    movingTimeSeconds: Long,
    elevationGainMeters: Double? = null,
    averageSpeedMps: Double? = null,
    maxSpeedMps: Double? = null,
    estimatedMaxPowerWatts: Double? = null,
) = RideSummary(
    id = id,
    name = id,
    startTimeMillis = 0,
    endTimeMillis = movingTimeSeconds * 1000,
    distanceMeters = distanceMeters,
    movingTimeSeconds = movingTimeSeconds,
    totalTimeSeconds = movingTimeSeconds,
    averageSpeedMps = averageSpeedMps,
    maxSpeedMps = maxSpeedMps,
    elevationGainMeters = elevationGainMeters,
    maxElevationMeters = null,
    averageGradePercent = null,
    maxGradePercent = null,
    estimatedAveragePowerWatts = null,
    estimatedMaxPowerWatts = estimatedMaxPowerWatts,
)

class PersonalRecordsCalculatorTest {

    @Test
    fun `no rides yields empty records`() {
        assertThat(PersonalRecordsCalculator.calculate(emptyList())).isEqualTo(PersonalRecords.EMPTY)
    }

    @Test
    fun `longest ride by distance is identified`() {
        val rides = listOf(
            ride("short", distanceMeters = 5_000.0, movingTimeSeconds = 600),
            ride("long", distanceMeters = 50_000.0, movingTimeSeconds = 6000),
        )
        val records = PersonalRecordsCalculator.calculate(rides)
        assertThat(records.longestRideId).isEqualTo("long")
        assertThat(records.longestRideDistanceMeters).isEqualTo(50_000.0)
    }

    @Test
    fun `rides with incomplete data are excluded entirely not treated as zero`() {
        val rides = listOf(
            ride("incomplete", distanceMeters = 0.0, movingTimeSeconds = 0),
            ride("valid", distanceMeters = 10_000.0, movingTimeSeconds = 1200),
        )
        val records = PersonalRecordsCalculator.calculate(rides)
        assertThat(records.longestRideId).isEqualTo("valid")
    }

    @Test
    fun `elevation record only considers rides with known elevation`() {
        val rides = listOf(
            ride("no-elevation", distanceMeters = 10_000.0, movingTimeSeconds = 1000, elevationGainMeters = null),
            ride("with-elevation", distanceMeters = 10_000.0, movingTimeSeconds = 1000, elevationGainMeters = 850.0),
        )
        val records = PersonalRecordsCalculator.calculate(rides)
        assertThat(records.highestElevationGainRideId).isEqualTo("with-elevation")
        assertThat(records.highestElevationGainMeters).isEqualTo(850.0)
    }

    @Test
    fun `power record uses estimated max power and is null when no ride has an estimate`() {
        val rides = listOf(ride("no-power", distanceMeters = 10_000.0, movingTimeSeconds = 1000, estimatedMaxPowerWatts = null))
        val records = PersonalRecordsCalculator.calculate(rides)
        assertThat(records.highestEstimatedPowerWatts).isNull()
    }

    @Test
    fun `longest duration record is independent from longest distance`() {
        val rides = listOf(
            ride("far-but-fast", distanceMeters = 40_000.0, movingTimeSeconds = 3600),
            ride("slow-but-long-duration", distanceMeters = 30_000.0, movingTimeSeconds = 7200),
        )
        val records = PersonalRecordsCalculator.calculate(rides)
        assertThat(records.longestRideDurationRideId).isEqualTo("slow-but-long-duration")
        assertThat(records.longestRideId).isEqualTo("far-but-fast")
    }
}
