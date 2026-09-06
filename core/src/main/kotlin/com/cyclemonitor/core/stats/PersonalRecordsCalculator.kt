package com.cyclemonitor.core.stats

import com.cyclemonitor.core.model.RideSummary

data class PersonalRecords(
    val longestRideDistanceMeters: Double?,
    val longestRideId: String?,
    val highestElevationGainMeters: Double?,
    val highestElevationGainRideId: String?,
    val highestAverageSpeedMps: Double?,
    val highestAverageSpeedRideId: String?,
    val highestMaxSpeedMps: Double?,
    val highestMaxSpeedRideId: String?,
    val highestEstimatedPowerWatts: Double?,
    val highestEstimatedPowerRideId: String?,
    val longestRideDurationSeconds: Long?,
    val longestRideDurationRideId: String?,
) {
    companion object {
        val EMPTY = PersonalRecords(null, null, null, null, null, null, null, null, null, null, null, null)
    }
}

/**
 * Computes personal-best records strictly from valid, completed rides
 * ([RideSummary.isValidForStatistics]). A ride with missing/corrupt data is simply excluded from
 * consideration for that record rather than treated as a zero -- records are never invented from
 * incomplete data.
 */
object PersonalRecordsCalculator {

    fun calculate(rides: List<RideSummary>): PersonalRecords {
        val valid = rides.filter { it.isValidForStatistics }
        if (valid.isEmpty()) return PersonalRecords.EMPTY

        val longestDistance = valid.maxByOrNull { it.distanceMeters }
        val longestDuration = valid.maxByOrNull { it.movingTimeSeconds }
        val highestElevation = valid.filter { it.elevationGainMeters != null }.maxByOrNull { it.elevationGainMeters!! }
        val highestAverageSpeed = valid.filter { it.averageSpeedMps != null }.maxByOrNull { it.averageSpeedMps!! }
        val highestMaxSpeed = valid.filter { it.maxSpeedMps != null }.maxByOrNull { it.maxSpeedMps!! }
        val highestPower = valid.filter { it.estimatedMaxPowerWatts != null }.maxByOrNull { it.estimatedMaxPowerWatts!! }

        return PersonalRecords(
            longestRideDistanceMeters = longestDistance?.distanceMeters,
            longestRideId = longestDistance?.id,
            highestElevationGainMeters = highestElevation?.elevationGainMeters,
            highestElevationGainRideId = highestElevation?.id,
            highestAverageSpeedMps = highestAverageSpeed?.averageSpeedMps,
            highestAverageSpeedRideId = highestAverageSpeed?.id,
            highestMaxSpeedMps = highestMaxSpeed?.maxSpeedMps,
            highestMaxSpeedRideId = highestMaxSpeed?.id,
            highestEstimatedPowerWatts = highestPower?.estimatedMaxPowerWatts,
            highestEstimatedPowerRideId = highestPower?.id,
            longestRideDurationSeconds = longestDuration?.movingTimeSeconds,
            longestRideDurationRideId = longestDuration?.id,
        )
    }
}
