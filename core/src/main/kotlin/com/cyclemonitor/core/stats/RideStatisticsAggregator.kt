package com.cyclemonitor.core.stats

import com.cyclemonitor.core.model.RideSummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class PeriodStatistics(
    val distanceMeters: Double,
    val movingTimeSeconds: Long,
    val rideCount: Int,
    val elevationGainMeters: Double?,
    val averageSpeedMps: Double?,
) {
    companion object {
        val EMPTY = PeriodStatistics(0.0, 0L, 0, null, null)
    }
}

/**
 * Aggregates completed rides over an arbitrary time range. Deliberately takes a raw
 * [start, end) window rather than a fixed enum of periods, so it serves both the calendar-aligned
 * "This Week / This Month / This Year / All Time" cards and the rolling "7 days / 30 days / 1
 * year / All time" filters with one implementation. Only [RideSummary.isValidForStatistics]
 * rides are counted -- incomplete/corrupt rides never contribute to statistics.
 */
object RideStatisticsAggregator {

    fun aggregate(
        rides: List<RideSummary>,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long,
    ): PeriodStatistics {
        val filtered = rides.filter {
            it.isValidForStatistics &&
                it.startTimeMillis < endExclusiveMillis &&
                (startInclusiveMillis == null || it.startTimeMillis >= startInclusiveMillis)
        }
        if (filtered.isEmpty()) return PeriodStatistics.EMPTY

        val totalDistance = filtered.sumOf { it.distanceMeters }
        val totalMovingTime = filtered.sumOf { it.movingTimeSeconds }
        val elevationValues = filtered.mapNotNull { it.elevationGainMeters }
        val totalElevation = if (elevationValues.isEmpty()) null else elevationValues.sum()
        val averageSpeed = if (totalMovingTime > 0) totalDistance / totalMovingTime else null

        return PeriodStatistics(
            distanceMeters = totalDistance,
            movingTimeSeconds = totalMovingTime,
            rideCount = filtered.size,
            elevationGainMeters = totalElevation,
            averageSpeedMps = averageSpeed,
        )
    }

    fun startOfDay(instant: Instant, zone: ZoneId): Instant =
        instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

    fun startOfWeek(instant: Instant, zone: ZoneId): Instant =
        instant.atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant()

    fun startOfMonth(instant: Instant, zone: ZoneId): Instant =
        instant.atZone(zone).toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(zone).toInstant()

    fun startOfYear(instant: Instant, zone: ZoneId): Instant =
        instant.atZone(zone).toLocalDate()
            .withDayOfYear(1)
            .atStartOfDay(zone).toInstant()
}
