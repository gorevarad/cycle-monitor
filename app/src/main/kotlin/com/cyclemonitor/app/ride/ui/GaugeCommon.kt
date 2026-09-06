package com.cyclemonitor.app.ride.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.AnimationIntensity

/** Gauge sweep/number transitions respect the user's animation setting; RIDING never hides data
 * behind motion, it only controls how eagerly the arc/number chase the (already display-smoothed)
 * value -- see ride/ui/RideViewModel.kt for the raw -> display -> animation separation. */
internal fun gaugeAnimationDurationMillis(intensity: AnimationIntensity): Int = when (intensity) {
    AnimationIntensity.OFF -> 0
    AnimationIntensity.MINIMAL -> 150
    AnimationIntensity.STANDARD -> 400
    AnimationIntensity.DYNAMIC -> 650
}

/**
 * On first display, sweeps the gauge arc 0 -> max -> the real value once (purely visual --
 * the underlying sensor value this wraps is untouched), then tracks [targetFraction] normally
 * for every later update. Skipped entirely when animations are off.
 */
@Composable
internal fun rememberCalibratedGaugeFraction(targetFraction: Float, animationIntensity: AnimationIntensity): Float {
    var calibrated by remember { mutableStateOf(animationIntensity == AnimationIntensity.OFF) }
    val calibrationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (calibrated) return@LaunchedEffect
        calibrationProgress.animateTo(1f, tween(450))
        calibrationProgress.animateTo(0f, tween(250))
        calibrated = true
    }

    val chaseTarget = if (calibrated) targetFraction else calibrationProgress.value
    val animatedFraction by animateFloatAsState(
        targetValue = chaseTarget,
        animationSpec = tween(durationMillis = gaugeAnimationDurationMillis(animationIntensity)),
        label = "gaugeFraction",
    )
    return animatedFraction
}

@Composable
internal fun SecondaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CycleColors.TextSecondary)
    }
}
