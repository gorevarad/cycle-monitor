package com.cyclemonitor.app.ride.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.cyclemonitor.core.model.AnimationIntensity
import kotlinx.coroutines.delay

/**
 * Which dashboard elements have faded in yet, implementing the product's startup sequence
 * (background -> speed gauge -> power gauge -> map) as a staggered reveal of the real dashboard
 * rather than a separate splash screen -- so nothing ever delays GPS/ride-recording startup, and
 * the "gauge calibration sweep" (see SpeedGauge/PowerGauge) plays right where the gauge lives.
 * Plays once per process (a rider re-rotating the phone shouldn't re-trigger it).
 */
private object DashboardIntroState {
    var hasPlayed = false
}

data class DashboardIntroVisibility(val speed: Boolean, val map: Boolean, val power: Boolean, val metrics: Boolean)

@Composable
fun rememberDashboardIntroVisibility(animationIntensity: AnimationIntensity): DashboardIntroVisibility {
    val alreadyPlayed = DashboardIntroState.hasPlayed || animationIntensity == AnimationIntensity.OFF
    var speed by remember { mutableStateOf(alreadyPlayed) }
    var map by remember { mutableStateOf(alreadyPlayed) }
    var power by remember { mutableStateOf(alreadyPlayed) }
    var metrics by remember { mutableStateOf(alreadyPlayed) }

    LaunchedEffect(Unit) {
        if (alreadyPlayed) return@LaunchedEffect
        val stepDelay = when (animationIntensity) {
            AnimationIntensity.MINIMAL -> 80L
            AnimationIntensity.STANDARD -> 150L
            AnimationIntensity.DYNAMIC -> 220L
            AnimationIntensity.OFF -> 0L
        }
        speed = true
        delay(stepDelay)
        map = true
        delay(stepDelay)
        power = true
        delay(stepDelay)
        metrics = true
        DashboardIntroState.hasPlayed = true
    }

    return DashboardIntroVisibility(speed, map, power, metrics)
}

@Composable
fun IntroVisible(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 })) {
        content()
    }
}
