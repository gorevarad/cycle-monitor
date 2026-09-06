package com.cyclemonitor.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded point of a ride, preserving both the raw location sample and the values derived
 * from it at record time. This is what lets a ride's route, graphs, and statistics all be
 * reconstructed later without having re-recorded anything.
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("rideId")],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val rideId: String,
    val sequence: Int,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val reportedSpeedMps: Float?,
    val speedAccuracyMps: Float?,
    val bearingDegrees: Float?,
    val altitudeMeters: Double?,
    val altitudeAccuracyMeters: Float?,
    val isMocked: Boolean,
    val cumulativeDistanceMeters: Double,
    val speedMps: Double,
    val gradePercent: Double?,
    val estimatedPowerWatts: Double?,
    /** Name of [com.cyclemonitor.core.power.EstimationConfidence]; stored as text for readability/debuggability. */
    val powerConfidence: String,
    val isPaused: Boolean,
)
