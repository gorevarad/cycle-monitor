package com.cyclemonitor.app.ride.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.AnimationIntensity
import java.util.Locale

@Composable
fun SpeedGauge(
    displaySpeed: Double?,
    unitSymbol: String,
    maxScale: Int,
    averageSpeed: Double?,
    maxSpeed: Double?,
    animationIntensity: AnimationIntensity,
    accentColor: Color = CycleColors.NavigationCyan,
    modifier: Modifier = Modifier,
) {
    val fraction = ((displaySpeed ?: 0.0) / maxScale).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = gaugeAnimationDurationMillis(animationIntensity)),
        label = "speedGaugeFraction",
    )

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        CircularGaugeArc(
            fraction = animatedFraction,
            color = accentColor,
            trackColor = CycleColors.OutlineSubtle,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displaySpeed?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                style = MaterialTheme.typography.displayLarge,
                color = CycleColors.TextPrimary,
            )
            Text(unitSymbol.uppercase(Locale.US), style = MaterialTheme.typography.labelMedium, color = CycleColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SecondaryStat("AVG", averageSpeed?.let { String.format(Locale.US, "%.1f", it) } ?: "--")
                SecondaryStat("MAX", maxSpeed?.let { String.format(Locale.US, "%.1f", it) } ?: "--")
            }
        }
    }
}
