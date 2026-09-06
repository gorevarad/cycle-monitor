package com.cyclemonitor.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY startTimeMillis DESC")
    fun observeAll(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getById(rideId: String): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ride: RideEntity)

    @Query("DELETE FROM rides WHERE id = :rideId")
    suspend fun deleteById(rideId: String)

    @Query("UPDATE rides SET name = :newName WHERE id = :rideId")
    suspend fun rename(rideId: String, newName: String)

    /** Used by the Data Retention setting -- deletes rides that finished before [cutoffMillis]. */
    @Query("DELETE FROM rides WHERE endTimeMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE rideId = :rideId ORDER BY sequence ASC")
    suspend fun getForRide(rideId: String): List<TrackPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("DELETE FROM track_points WHERE rideId = :rideId")
    suspend fun deleteForRide(rideId: String)

    @Transaction
    suspend fun replaceForRide(rideId: String, points: List<TrackPointEntity>) {
        deleteForRide(rideId)
        insertAll(points)
    }
}
