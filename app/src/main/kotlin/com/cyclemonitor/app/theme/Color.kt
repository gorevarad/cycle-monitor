package com.cyclemonitor.app.theme

import androidx.compose.ui.graphics.Color

/**
 * The app's dark-first, instrument-cluster color system. These are used both to build the
 * Material3 [androidx.compose.material3.ColorScheme] and directly by gauges/status chips that
 * need a specific semantic color (e.g. a GPS status dot) rather than a generic scheme role.
 *
 * Semantic meaning is always reinforced with text/icons, never color alone (see
 * ride/ui/GpsStatusIndicator.kt and power confidence labels).
 */
object CycleColors {
    val BackgroundCharcoal = Color(0xFF0A0C0E)
    val SurfaceCharcoal = Color(0xFF14171A)
    val SurfaceRaised = Color(0xFF1C2024)
    val OutlineSubtle = Color(0xFF2A2F34)

    val TextPrimary = Color(0xFFF5F7F8)
    val TextSecondary = Color(0xFFA6ADB4)
    val TextDisabled = Color(0xFF5C636A)

    /** Navigation / map / informational accent. */
    val NavigationCyan = Color(0xFF39D6E0)

    /** Good state: GPS good, normal effort, on-track. */
    val StatusGreen = Color(0xFF4CD97B)

    /** Caution: weak-ish GPS, moderate effort, non-critical warning. */
    val StatusAmber = Color(0xFFE8B23A)

    /** High effort / elevated zone. */
    val StatusOrange = Color(0xFFE87A3A)

    /** Critical: GPS unavailable, permission denied, recording failure. */
    val StatusRed = Color(0xFFE84C4C)
}
