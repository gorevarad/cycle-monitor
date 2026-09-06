package com.cyclemonitor.app.ride.ui

import java.util.Locale

/** Shared, null-safe display formatters used across the Ride, History, and Records screens.
 * Every one of these renders "N/A" for a null input rather than fabricating a zero. */
object Formatters {
    fun distance(value: Double?, unitSymbol: String): String =
        value?.let { String.format(Locale.US, "%.1f %s", it, unitSymbol) } ?: "N/A"

    fun elevation(value: Double?, unitSymbol: String): String =
        value?.let { String.format(Locale.US, "+%.0f %s", it, unitSymbol) } ?: "N/A"

    fun grade(percent: Double?): String =
        percent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"

    fun speed(value: Double?, unitSymbol: String): String =
        value?.let { String.format(Locale.US, "%.1f %s", it, unitSymbol) } ?: "N/A"

    fun power(watts: Double?): String = watts?.let { "EST. ${it.toInt()} W" } ?: "N/A"

    /** mm:ss for a sub-hour duration, h:mm:ss beyond that. */
    fun duration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}
