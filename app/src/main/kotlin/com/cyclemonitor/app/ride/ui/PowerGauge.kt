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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.AnimationIntensity
import com.cyclemonitor.core.power.EstimationConfidence
import java.util.Locale
import kotlin.math.roundToInt

/**
 * THE PHONE HAS NO POWER METER: this always renders the "EST." prefix and never presents the
 * number with more certainty than the underlying [EstimationConfidence] warrants -- LOW
 * confidence gets a "~" prefix and a visible "LOW CONFIDENCE" label; UNAVAILABLE shows no number
 * at all. See core/power/PowerEstimationEngine.kt for what drives each confidence level.
 */
@Composable
fun PowerGauge(
    displayPowerWatts: Double?,
    confidence: EstimationConfidence,
    maxScaleWatts: Int,
    power3sAvgWatts: Double?,
    averagePowerWatts: Double?,
    maxPowerWatts: Double?,
    animationIntensity: AnimationIntensity,
    accentColor: Color = CycleColors.StatusOrange,
    modifier: Modifier = Modifier,
) {
    val fraction = ((displayPowerWatts ?: 0.0) / maxScaleWatts).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = gaugeAnimationDurationMillis(animationIntensity)),
        label = "powerGaugeFraction",
    )
    val gaugeColor = if (confidence == EstimationConfidence.LOW) CycleColors.TextDisabled else accentColor

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        CircularGaugeArc(
            fraction = animatedFraction,
            color = gaugeColor,
            trackColor = CycleColors.OutlineSubtle,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text("EST. POWER", style = MaterialTheme.typography.labelMedium, color = CycleColors.TextSecondary)
            when (confidence) {
                EstimationConfidence.UNAVAILABLE -> Text(
                    "POWER ESTIMATE\nUNAVAILABLE",
                    style = MaterialTheme.typography.titleMedium,
                    color = CycleColors.TextDisabled,
                    textAlign = TextAlign.Center,
                )
                else -> {
                    val prefix = if (confidence == EstimationConfidence.LOW) "~" else ""
                    Text(
                        text = displayPowerWatts?.let { "$prefix${it.roundToInt()}" } ?: "--",
                        style = MaterialTheme.typography.displayLarge,
                        color = CycleColors.TextPrimary,
                    )
                    Text("W", style = MaterialTheme.typography.labelMedium, color = CycleColors.TextSecondary)
                    if (confidence == EstimationConfidence.LOW) {
                        Text(
                            "LOW CONFIDENCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CycleColors.StatusAmber,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryStat("3s AVG", power3sAvgWatts?.let { String.format(Locale.US, "%.0f", it) } ?: "--")
                SecondaryStat("AVG", averagePowerWatts?.let { String.format(Locale.US, "%.0f", it) } ?: "--")
                SecondaryStat("MAX", maxPowerWatts?.let { String.format(Locale.US, "%.0f", it) } ?: "--")
            }
        }
    }
}
