package com.cyclemonitor.app.data.repository

import com.cyclemonitor.app.data.db.RideDao
import com.cyclemonitor.app.data.db.RideEntity
import com.cyclemonitor.app.data.db.TrackPointDao
import com.cyclemonitor.app.data.db.TrackPointEntity
import com.cyclemonitor.core.model.LocationSample
import com.cyclemonitor.core.model.RideDetail
import com.cyclemonitor.core.model.RideSummary
import com.cyclemonitor.core.model.TrackPoint
import com.cyclemonitor.core.power.EstimationConfidence
import com.cyclemonitor.core.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [RideRepository]. Translates between `:core` domain models and Room entities. */
class RoomRideRepository(
    private val rideDao: RideDao,
    private val trackPointDao: TrackPointDao,
) : RideRepository {

    override fun observeRides(): Flow<List<RideSummary>> =
        rideDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getRideDetail(rideId: String): RideDetail? {
        val ride = rideDao.getById(rideId) ?: return null
        val points = trackPointDao.getForRide(rideId).map { it.toDomain() }
        return RideDetail(summary = ride.toDomain(), trackPoints = points)
    }

    override suspend fun saveRide(detail: RideDetail) {
        rideDao.upsert(detail.summary.toEntity())
        trackPointDao.replaceForRide(detail.summary.id, detail.trackPoints.map { it.toEntity(detail.summary.id) })
    }

    override suspend fun deleteRide(rideId: String) {
        rideDao.deleteById(rideId)
    }

    override suspend fun renameRide(rideId: String, newName: String) {
        rideDao.rename(rideId, newName)
    }
}

private fun RideEntity.toDomain() = RideSummary(
    id = id,
    name = name,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    distanceMeters = distanceMeters,
    movingTimeSeconds = movingTimeSeconds,
    totalTimeSeconds = totalTimeSeconds,
    averageSpeedMps = averageSpeedMps,
    maxSpeedMps = maxSpeedMps,
    elevationGainMeters = elevationGainMeters,
    maxElevationMeters = maxElevationMeters,
    averageGradePercent = averageGradePercent,
    maxGradePercent = maxGradePercent,
    estimatedAveragePowerWatts = estimatedAveragePowerWatts,
    estimatedMaxPowerWatts = estimatedMaxPowerWatts,
)

private fun RideSummary.toEntity() = RideEntity(
    id = id,
    name = name,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    distanceMeters = distanceMeters,
    movingTimeSeconds = movingTimeSeconds,
    totalTimeSeconds = totalTimeSeconds,
    averageSpeedMps = averageSpeedMps,
    maxSpeedMps = maxSpeedMps,
    elevationGainMeters = elevationGainMeters,
    maxElevationMeters = maxElevationMeters,
    averageGradePercent = averageGradePercent,
    maxGradePercent = maxGradePercent,
    estimatedAveragePowerWatts = estimatedAveragePowerWatts,
    estimatedMaxPowerWatts = estimatedMaxPowerWatts,
)

private fun TrackPointEntity.toDomain() = TrackPoint(
    sequence = sequence,
    sample = LocationSample(
        timestampMillis = timestampMillis,
        latitude = latitude,
        longitude = longitude,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        reportedSpeedMps = reportedSpeedMps,
        speedAccuracyMps = speedAccuracyMps,
        bearingDegrees = bearingDegrees,
        altitudeMeters = altitudeMeters,
        altitudeAccuracyMeters = altitudeAccuracyMeters,
        isMocked = isMocked,
    ),
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    speedMps = speedMps,
    gradePercent = gradePercent,
    estimatedPowerWatts = estimatedPowerWatts,
    powerConfidence = runCatching { EstimationConfidence.valueOf(powerConfidence) }.getOrDefault(EstimationConfidence.UNAVAILABLE),
    isPaused = isPaused,
)

private fun TrackPoint.toEntity(rideId: String) = TrackPointEntity(
    rideId = rideId,
    sequence = sequence,
    timestampMillis = sample.timestampMillis,
    latitude = sample.latitude,
    longitude = sample.longitude,
    horizontalAccuracyMeters = sample.horizontalAccuracyMeters,
    reportedSpeedMps = sample.reportedSpeedMps,
    speedAccuracyMps = sample.speedAccuracyMps,
    bearingDegrees = sample.bearingDegrees,
    altitudeMeters = sample.altitudeMeters,
    altitudeAccuracyMeters = sample.altitudeAccuracyMeters,
    isMocked = sample.isMocked,
    cumulativeDistanceMeters = cumulativeDistanceMeters,
    speedMps = speedMps,
    gradePercent = gradePercent,
    estimatedPowerWatts = estimatedPowerWatts,
    powerConfidence = powerConfidence.name,
    isPaused = isPaused,
)
