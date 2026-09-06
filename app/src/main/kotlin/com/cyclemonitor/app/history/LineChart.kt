package com.cyclemonitor.app.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors

/**
 * A minimal, dependency-free line chart: no external charting library, just Canvas. Supports
 * tap/drag to inspect a point's value (spec: "make graphs interactive where practical"). Null
 * values in [points] are skipped when drawing (gaps in the data, e.g. missing elevation for part
 * of a ride) rather than interpolated into a fabricated value.
 */
@Composable
fun LineChart(
    title: String,
    points: List<Float?>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier,
    color: Color = CycleColors.NavigationCyan,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val validPoints = points.mapIndexedNotNull { index, value -> value?.let { index to it } }

    Box(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = CycleColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
        if (validPoints.size < 2) {
            Text("Not enough data", color = CycleColors.TextDisabled, modifier = Modifier.padding(24.dp).align(Alignment.Center))
            return@Box
        }

        val minY = validPoints.minOf { it.second }
        val maxY = validPoints.maxOf { it.second }
        val range = (maxY - minY).takeIf { it > 0.0001f } ?: 1f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 24.dp)
                .pointerInput(points) {
                    detectTapGestures { offset -> selectedIndex = nearestIndex(offset, size.width, points.size) }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDrag = { change, _ -> selectedIndex = nearestIndex(change.position, size.width, points.size) },
                    )
                },
        ) {
            val path = Path()
            var started = false
            validPoints.forEach { (index, value) ->
                val x = index.toFloat() / (points.size - 1).coerceAtLeast(1) * size.width
                val y = size.height - ((value - minY) / range) * size.height
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))

            selectedIndex?.let { idx ->
                val value = points.getOrNull(idx) ?: return@let
                val x = idx.toFloat() / (points.size - 1).coerceAtLeast(1) * size.width
                drawLine(
                    color = CycleColors.TextSecondary,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f,
                )
            }
        }

        selectedIndex?.let { idx ->
            points.getOrNull(idx)?.let { value ->
                Text(
                    valueFormatter(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = CycleColors.TextPrimary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                )
            }
        }
    }
}

private fun nearestIndex(offset: Offset, widthPx: Int, count: Int): Int {
    if (count <= 1) return 0
    val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
    return (fraction * (count - 1)).toInt()
}
