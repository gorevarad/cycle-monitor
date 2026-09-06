package com.cyclemonitor.app.routing

/**
 * Decodes Google's "encoded polyline algorithm format" as used by the Directions API's
 * `overview_polyline`/`polyline` fields. Standard, well-documented algorithm --
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
object PolylineDecoder {
    fun decode(encoded: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val deltaLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += deltaLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val deltaLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += deltaLng

            points += (lat / 1e5) to (lng / 1e5)
        }
        return points
    }
}
