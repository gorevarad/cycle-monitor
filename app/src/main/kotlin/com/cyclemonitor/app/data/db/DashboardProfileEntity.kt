package com.cyclemonitor.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved dashboard layout. The four built-in presets (Road/Climb/Race/Casual) are seeded as
 * ordinary rows with [isCustom] = false so "reset to defaults" and "delete" can treat presets and
 * user-created profiles uniformly except that presets can't be deleted -- see
 * DashboardProfileRepository.seedPresetsIfEmpty and DashboardProfileDao.deleteCustom.
 */
@Entity(tableName = "dashboard_profiles")
data class DashboardProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Ordered, comma-separated [com.cyclemonitor.core.model.DashboardMetric] names -- order is the display order. */
    val metricsCsv: String,
    val speedGaugeMaxKmh: Int,
    val powerGaugeMaxWatts: Int,
    val accentColorArgb: Int,
    val orientationBehavior: String,
    val animationIntensity: String,
    val isCustom: Boolean,
)
