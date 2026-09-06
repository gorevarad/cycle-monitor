package com.cyclemonitor.app.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Projects a ride's raw lat/lon coordinates into a normalized 0..1 square, preserving aspect
 * ratio, so a route can be drawn as a clean stylized line -- used by Ride Details and Flex Mode
 * alike. Deliberately NOT a screenshot of Google Maps (see FLEX ROUTE DESIGN in the product
 * brief): this is a simple equirectangular projection appropriate for a single short ride's
 * extent, not a general-purpose map projection.
 */
object RouteStylizer {
    data class NormalizedPoint(val x: Float, val y: Float)

    fun normalize(points: List<Pair<Double, Double>>): List<NormalizedPoint> {
        if (points.isEmpty()) return emptyList()
        val lats = points.map { it.first }
        val lons = points.map { it.second }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLon = lons.min()
        val maxLon = lons.max()
        val latRange = (maxLat - minLat).takeIf { it > MIN_RANGE_DEGREES } ?: MIN_RANGE_DEGREES
        val lonRange = (maxLon - minLon).takeIf { it > MIN_RANGE_DEGREES } ?: MIN_RANGE_DEGREES
        // Use the larger of the two ranges for both axes so the route isn't stretched.
        val range = maxOf(latRange, lonRange)
        val latCenter = (minLat + maxLat) / 2.0
        val lonCenter = (minLon + maxLon) / 2.0
        return points.map { (lat, lon) ->
            val x = 0.5f + ((lon - lonCenter) / range).toFloat()
            // Screen y grows downward; latitude grows northward, so invert.
            val y = 0.5f - ((lat - latCenter) / range).toFloat()
            NormalizedPoint(x, y)
        }
    }

    private const val MIN_RANGE_DEGREES = 0.0002 // ~20m, avoids a division blowup for a near-stationary ride
}

@Composable
fun StaticRoutePreview(
    points: List<Pair<Double, Double>>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthPx: Float = 8f,
    dashed: Boolean = false,
) {
    val normalized = remember(points) { RouteStylizer.normalize(points) }
    Canvas(modifier = modifier) {
        if (normalized.size < 2) return@Canvas
        val margin = strokeWidthPx * 2
        val path = Path()
        normalized.forEachIndexed { index, point ->
            val x = margin + point.x * (size.width - margin * 2)
            val y = margin + point.y * (size.height - margin * 2)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(strokeWidthPx * 2, strokeWidthPx * 2)) else null,
            ),
        )
    }
}
