package com.cyclemonitor.app.ride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.GpsQuality
import java.util.Locale

/**
 * Never claims accuracy the platform hasn't actually reported -- [GpsQuality.UNAVAILABLE] and a
 * null accuracy always render as a plain "GPS ● UNAVAILABLE" with no distance figure. Color alone
 * never carries the meaning; the label text does the real communicating.
 */
@Composable
fun GpsStatusIndicator(
    quality: GpsQuality,
    accuracyMeters: Float?,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (quality) {
        GpsQuality.GOOD -> CycleColors.StatusGreen to "GOOD"
        GpsQuality.MODERATE -> CycleColors.StatusAmber to "MODERATE"
        GpsQuality.WEAK -> CycleColors.StatusOrange to "WEAK"
        GpsQuality.UNAVAILABLE -> CycleColors.StatusRed to "UNAVAILABLE"
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        StatusDot(color)
        Text(
            text = buildString {
                append("GPS ")
                append(label)
                if (accuracyMeters != null) append(String.format(Locale.US, " ±%.0fm", accuracyMeters))
            },
            style = MaterialTheme.typography.labelMedium,
            color = CycleColors.TextSecondary,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}
