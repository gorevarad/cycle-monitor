package com.cyclemonitor.app.ride.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
internal fun SecondaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CycleColors.TextSecondary)
    }
}
