package com.cyclemonitor.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A completed (or in-progress-but-checkpointed) ride's summary fields. Aggregate stats
 * (distance, elevation gain, etc.) are denormalized directly onto this row rather than kept in
 * a separate "RideStatistics" table, since they're derived once when the ride finishes and never
 * recomputed independently of their ride -- the full [TrackPointEntity] rows are what's kept
 * to allow re-deriving anything later, per the "never store only the final statistics" rule.
 */
@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val distanceMeters: Double,
    val movingTimeSeconds: Long,
    val totalTimeSeconds: Long,
    val averageSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val elevationGainMeters: Double?,
    val maxElevationMeters: Double?,
    val averageGradePercent: Double?,
    val maxGradePercent: Double?,
    val estimatedAveragePowerWatts: Double?,
    val estimatedMaxPowerWatts: Double?,
)
