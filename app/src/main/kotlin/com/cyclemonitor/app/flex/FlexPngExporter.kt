package com.cyclemonitor.app.flex

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cyclemonitor.app.data.settings.UserSettings
import com.cyclemonitor.app.map.RouteStylizer
import com.cyclemonitor.app.ride.ui.Formatters
import com.cyclemonitor.core.model.RideSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * Renders a Flex overlay straight to an ARGB_8888 android.graphics.Bitmap and saves it as a
 * transparent PNG. Uses plain android.graphics (not a captured Compose composable) so the export
 * path has no dependency on Compose's snapshot/graphics-layer APIs -- simpler and more predictable
 * for a file that has to be pixel-correct. FlexScreen.kt's live preview draws the same fields with
 * Compose Canvas for on-screen use; both read from the same [FlexConfig].
 *
 * The route is NOT a screenshot of Google Maps -- it's a stylized line drawn from the ride's own
 * recorded coordinates via [RouteStylizer], per the product's "clean, minimal route graphic"
 * requirement.
 */
object FlexPngExporter {

    suspend fun export(
        context: Context,
        config: FlexConfig,
        summary: RideSummary,
        routePoints: List<Pair<Double, Double>>,
        settings: UserSettings,
        widthPx: Int,
        heightPx: Int,
    ): Result<Uri> = withContext(Dispatchers.Default) {
        try {
            // ARGB_8888 bitmaps from createBitmap start fully transparent (all-zero pixels) --
            // nothing is ever drawn as an opaque background rectangle, so transparency survives.
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawOverlay(canvas, widthPx, heightPx, config, summary, routePoints, settings)
            val uri = saveToStorage(context, bitmap)
            Result.success(uri)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    private fun drawOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: FlexConfig,
        summary: RideSummary,
        routePoints: List<Pair<Double, Double>>,
        settings: UserSettings,
    ) {
        val margin = width * 0.08f

        if (config.showRoute && routePoints.size >= 2) {
            drawRoute(canvas, RectF(margin, margin, width - margin, height * 0.55f), config, routePoints)
        }

        val lines = buildList {
            if (config.showRideName) add(summary.name.uppercase(Locale.US) to true)
            if (config.showDistance) {
                add(Formatters.distance(settings.distanceUnit.fromMeters(summary.distanceMeters), settings.distanceUnit.symbol()).uppercase(Locale.US) to false)
            }
            if (config.showSpeed) {
                add(Formatters.speed(summary.averageSpeedMps?.let { settings.speedUnit.fromMetersPerSecond(it) }, settings.speedUnit.symbol()).uppercase(Locale.US) to false)
            }
            if (config.showTime) add(Formatters.duration(summary.movingTimeSeconds) to false)
            if (config.showElevation) {
                add(Formatters.elevation(summary.elevationGainMeters?.let { settings.elevationUnit.fromMeters(it) }, settings.elevationUnit.symbol()) to false)
            }
            if (config.showPower) add(Formatters.power(summary.estimatedAveragePowerWatts) to false)
        }

        var y = height * 0.72f
        lines.forEach { (text, isTitle) ->
            val paint = textPaint(width, config, isTitle)
            canvas.drawText(text, margin, y, paint)
            y += paint.textSize * 1.35f
        }
    }

    private fun textPaint(width: Int, config: FlexConfig, isTitle: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * config.textOpacity).toInt().coerceIn(0, 255)
        textSize = width * (if (isTitle) 0.05f else 0.085f) * config.fontScale
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }

    private fun drawRoute(canvas: Canvas, bounds: RectF, config: FlexConfig, points: List<Pair<Double, Double>>) {
        val normalized = RouteStylizer.normalize(points)
        if (normalized.size < 2) return
        val path = Path()
        normalized.forEachIndexed { index, point ->
            val x = bounds.left + point.x * bounds.width()
            val y = bounds.top + point.y * bounds.height()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = config.routeStrokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.CYAN
            alpha = (255 * config.routeOpacity).toInt().coerceIn(0, 255)
        }
        canvas.drawPath(path, paint)
    }

    private fun saveToStorage(context: Context, bitmap: Bitmap): Uri {
        val filename = "flex_${System.currentTimeMillis()}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CycleMonitor")
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("PNG encode failed")
            } ?: throw IOException("Could not open output stream for $uri")
            uri
        } else {
            // Pre-Q fallback: app-specific external storage needs no runtime permission on any
            // API level, at the cost of not appearing in the system Gallery automatically.
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "CycleMonitor").apply { mkdirs() }
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("PNG encode failed")
            }
            Uri.fromFile(file)
        }
    }
}
