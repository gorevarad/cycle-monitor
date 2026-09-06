package com.cyclemonitor.app.ride.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.AnimationIntensity
import kotlinx.coroutines.delay

private fun stepDelayMillis(animationIntensity: AnimationIntensity): Long = when (animationIntensity) {
    AnimationIntensity.OFF -> 0L
    AnimationIntensity.MINIMAL -> 220L
    AnimationIntensity.STANDARD -> 400L
    AnimationIntensity.DYNAMIC -> 550L
}

/**
 * Short, skippable STARTING -> GPS READY -> DATA READY -> RIDING sequence shown while
 * [com.cyclemonitor.core.ride.RideState.Starting]. Purely cosmetic: the recording service has
 * already begun acquiring location and does not wait on this animation for anything.
 */
@Composable
fun RideStartOverlay(animationIntensity: AnimationIntensity, onFinished: () -> Unit) {
    val steps = remember { listOf("STARTING", "GPS READY", "DATA READY", "RIDING") }
    var index by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val delayMillis = stepDelayMillis(animationIntensity)
        if (delayMillis == 0L) {
            onFinished()
            return@LaunchedEffect
        }
        for (i in steps.indices) {
            index = i
            delay(delayMillis)
        }
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(CycleColors.BackgroundCharcoal.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = index, label = "rideStartStep") { i ->
            Text(steps[i], style = MaterialTheme.typography.headlineMedium, color = CycleColors.NavigationCyan)
        }
    }
}

/**
 * Brief ride-complete summary (distance -> avg speed -> elevation -> power -> "RIDE COMPLETE")
 * shown while the ride is FINISHING/COMPLETED, before the caller navigates to Ride Details.
 */
@Composable
fun RideFinishOverlay(summary: RideUiState, animationIntensity: AnimationIntensity, onFinished: () -> Unit) {
    // Deliberately captured once (Unit key, not `summary`): the ride's live stats barely change
    // between FINISHING and COMPLETED, and re-keying on every recomposition of `summary` would
    // restart this short animation each time -- see RideViewModel's finishSummary snapshot.
    val lines = remember(Unit) {
        listOfNotNull(
            Formatters.distance(summary.distance, summary.distanceUnit.symbol()),
            summary.averageSpeed?.let { Formatters.speed(it, summary.speedUnit.symbol()) },
            summary.elevationGain?.let { Formatters.elevation(it, summary.elevationUnit.symbol()) },
            summary.averagePowerWatts?.let { Formatters.power(it) },
            "RIDE COMPLETE",
        )
    }
    var index by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val delayMillis = stepDelayMillis(animationIntensity).coerceAtLeast(300L)
        for (i in lines.indices) {
            index = i
            delay(delayMillis)
        }
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(CycleColors.BackgroundCharcoal.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(targetState = index.coerceIn(0, lines.lastIndex), label = "rideFinishStep") { i ->
                Text(
                    lines[i],
                    style = if (lines[i] == "RIDE COMPLETE") MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                    color = CycleColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}
