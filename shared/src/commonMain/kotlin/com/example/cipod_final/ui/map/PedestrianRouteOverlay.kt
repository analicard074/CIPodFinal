package com.example.cipod_final.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cipod_final.routing.GeoPoint
import com.example.cipod_final.routing.PedestrianRouteResult
import com.example.cipod_final.ui.theme.ChannelBlue
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox

@Composable
@MaplibreComposable
internal fun PedestrianRouteOverlay(route: PedestrianRouteResult?) {
    val available = route as? PedestrianRouteResult.Available ?: return
    if (available.points.size < 2) return
    val geoJson = remember(available.points) {
        GeoJsonData.JsonString(routeGeoJson(available.points))
    }
    val source = rememberGeoJsonSource(geoJson)

    // Keep the route geometry in one source; draw a wider white stroke beneath the blue route.
    LineLayer(
        id = "campus-pedestrian-route-outline",
        source = source,
        color = const(Color.White),
        width = const(11.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round)
    )
    LineLayer(
        id = "campus-pedestrian-route",
        source = source,
        color = const(Color(0xFF1677F5)),
        width = const(6.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round)
    )

    val endpoint = available.points.last()
    val endpointData = remember(endpoint) {
        GeoJsonData.JsonString(destinationGeoJson(endpoint))
    }
    val endpointSource = rememberGeoJsonSource(endpointData)
    CircleLayer(
        id = "campus-pedestrian-entrance-approach",
        source = endpointSource,
        radius = const(10.dp),
        color = const(ChannelBlue),
        strokeColor = const(Color.White),
        strokeWidth = const(4.dp)
    )
}

internal fun routeGeoJson(points: List<GeoPoint>): String {
    require(points.size >= 2)
    val positions = points.joinToString(",") { "[${it.longitude},${it.latitude}]" }
    return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$positions]}}]}"""
}

internal fun destinationGeoJson(point: GeoPoint): String =
    """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]}}]}"""

internal fun routeBounds(points: List<GeoPoint>): BoundingBox {
    require(points.isNotEmpty())
    return BoundingBox(
        west = points.minOf { it.longitude },
        south = points.minOf { it.latitude },
        east = points.maxOf { it.longitude },
        north = points.maxOf { it.latitude }
    )
}
