package com.cyclemonitor.core.repository

import com.cyclemonitor.core.model.RideDetail
import com.cyclemonitor.core.model.RideSummary
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for rides. The production implementation is backed by Room (see the
 * `:app` module); tests and previews can substitute an in-memory fake without touching a
 * database. Keeping this interface in `:core` keeps ride-recording/statistics logic decoupled
 * from Android/Room entirely.
 */
interface RideRepository {
    fun observeRides(): Flow<List<RideSummary>>

    suspend fun getRideDetail(rideId: String): RideDetail?

    suspend fun saveRide(detail: RideDetail)

    suspend fun deleteRide(rideId: String)

    suspend fun renameRide(rideId: String, newName: String)
}
