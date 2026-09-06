package com.cyclemonitor.app.flex

enum class FlexPreset { MINIMAL, FULL, RACE, ROUTE, CUSTOM }

enum class FlexAspectRatio(val widthRatio: Float, val heightRatio: Float, val label: String) {
    PORTRAIT_9_16(9f, 16f, "9:16"),
    LANDSCAPE_16_9(16f, 9f, "16:9"),
    SQUARE_1_1(1f, 1f, "1:1"),
    PORTRAIT_4_5(4f, 5f, "4:5"),
}

/**
 * Everything needed to render a Flex overlay, shared by the live preview (Compose) and the PNG
 * exporter (plain android.graphics) so both draw from the same configuration -- see
 * FlexScreen.kt and FlexPngExporter.kt.
 */
data class FlexConfig(
    val preset: FlexPreset = FlexPreset.FULL,
    val aspectRatio: FlexAspectRatio = FlexAspectRatio.PORTRAIT_9_16,
    val showRoute: Boolean = true,
    val showDistance: Boolean = true,
    val showSpeed: Boolean = true,
    val showTime: Boolean = true,
    val showElevation: Boolean = true,
    val showPower: Boolean = false,
    val showRideName: Boolean = false,
    val showDate: Boolean = false,
    val fontScale: Float = 1.0f,
    val routeStrokeWidthPx: Float = 10f,
    val routeOpacity: Float = 1.0f,
    val textOpacity: Float = 1.0f,
) {
    companion object {
        fun forPreset(preset: FlexPreset, previous: FlexConfig = FlexConfig()): FlexConfig = when (preset) {
            FlexPreset.MINIMAL -> previous.copy(
                preset = preset, showRoute = false, showDistance = true, showSpeed = true, showTime = true,
                showElevation = false, showPower = false,
            )
            FlexPreset.FULL -> previous.copy(
                preset = preset, showRoute = true, showDistance = true, showSpeed = true, showTime = true,
                showElevation = true, showPower = true,
            )
            FlexPreset.RACE -> previous.copy(
                preset = preset, showRoute = false, showDistance = true, showSpeed = false, showTime = true,
                showElevation = false, showPower = false, fontScale = 1.4f,
            )
            FlexPreset.ROUTE -> previous.copy(
                preset = preset, showRoute = true, showDistance = true, showSpeed = false, showTime = false,
                showElevation = true, showPower = false,
            )
            FlexPreset.CUSTOM -> previous.copy(preset = preset)
        }
    }
}
