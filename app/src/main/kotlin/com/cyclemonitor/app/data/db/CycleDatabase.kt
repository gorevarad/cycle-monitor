package com.cyclemonitor.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RideEntity::class, TrackPointEntity::class, DashboardProfileEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun dashboardProfileDao(): DashboardProfileDao

    companion object {
        fun build(context: Context): CycleDatabase =
            Room.databaseBuilder(context, CycleDatabase::class.java, "cycle_monitor.db")
                .build()
    }
}
