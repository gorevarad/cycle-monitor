package com.cyclemonitor.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardProfileDao {
    @Query("SELECT * FROM dashboard_profiles ORDER BY isCustom ASC, name ASC")
    fun observeAll(): Flow<List<DashboardProfileEntity>>

    @Query("SELECT * FROM dashboard_profiles WHERE id = :id")
    suspend fun getById(id: String): DashboardProfileEntity?

    @Query("SELECT COUNT(*) FROM dashboard_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: DashboardProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(profiles: List<DashboardProfileEntity>)

    /** Presets ([DashboardProfileEntity.isCustom] = false) can be edited but not deleted. */
    @Query("DELETE FROM dashboard_profiles WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: String)
}
