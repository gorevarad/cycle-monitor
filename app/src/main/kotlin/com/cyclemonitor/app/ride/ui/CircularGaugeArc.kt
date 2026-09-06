package com.cyclemonitor.app.ride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Automotive-style 270-degree gauge sweep, gap at the bottom, shared by the speed and power gauges. */
internal const val GAUGE_START_ANGLE = 135f
internal const val GAUGE_SWEEP_ANGLE = 270f

/**
 * Draws the gauge's background track plus a colored progress arc for [fraction] (0f..1f, already
 * clamped by the caller -- a gauge never draws past its printed maximum even if the underlying
 * value exceeds it, since that would misrepresent the configured scale).
 */
@Composable
internal fun CircularGaugeArc(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidthPx: Float = 24f,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        val inset = strokeWidthPx / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)

        drawArc(
            color = trackColor,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = GAUGE_SWEEP_ANGLE,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = GAUGE_SWEEP_ANGLE * fraction.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}
