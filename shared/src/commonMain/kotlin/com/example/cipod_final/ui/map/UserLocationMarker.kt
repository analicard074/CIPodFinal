package com.example.cipod_final.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.location.locationNowMillis
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val LocationBlue = Color(0xFF1976D2)

/** Pure rendering of a supplied snapshot. Does not start MapLibre's location/heading providers. */
@Composable
@MaplibreComposable
internal fun UserLocationMarker(reading: LocationReading?, expired: Boolean) {
    if (reading == null || LocationQualityPolicy.evaluate(reading, locationNowMillis()) == LocationQuality.Invalid) return
    val point = remember(reading) { GeoJsonData.JsonString(
        """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${reading.longitude},${reading.latitude}]}}"""
    ) }
    val area = remember(reading) { GeoJsonData.JsonString(accuracyArea(reading)) }
    val areaSource = rememberGeoJsonSource(area)
    val pointSource = rememberGeoJsonSource(point)
    FillLayer(
        id = "student-location-accuracy", source = areaSource,
        color = const(LocationBlue), opacity = const(if (expired) 0.05f else 0.12f),
        outlineColor = const(LocationBlue.copy(alpha = 0.4f))
    )
    CircleLayer(
        id = "student-location", source = pointSource,
        color = const(LocationBlue), radius = const(8.dp),
        opacity = const(if (expired) 0.4f else 1f),
        strokeColor = const(Color.White), strokeWidth = const(3.dp)
    )
}

/** Geodesic accuracy circle: its radius is in meters, not screen pixels. */
private fun accuracyArea(reading: LocationReading): String {
    val latitude = reading.latitude * PI / 180
    val longitude = reading.longitude * PI / 180
    val angularDistance = reading.accuracyMeters / 6_371_000.0
    val points = (0..64).joinToString(",") { index ->
        val bearing = (index % 64) * 2 * PI / 64
        val lat = asin((sin(latitude) * cos(angularDistance) +
            cos(latitude) * sin(angularDistance) * cos(bearing)).coerceIn(-1.0, 1.0))
        val lon = longitude + atan2(sin(bearing) * sin(angularDistance) * cos(latitude),
            cos(angularDistance) - sin(latitude) * sin(lat))
        val normalizedLongitude = ((lon * 180 / PI + 540) % 360) - 180
        "[$normalizedLongitude,${lat * 180 / PI}]"
    }
    return """{"type":"Feature","properties":{},"geometry":{"type":"Polygon","coordinates":[[$points]]}}"""
}
