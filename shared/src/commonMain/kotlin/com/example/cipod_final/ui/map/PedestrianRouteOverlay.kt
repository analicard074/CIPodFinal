package com.example.cipod_final.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.example.cipod_final.routing.GeoPoint
import com.example.cipod_final.routing.PedestrianRouteResult
import com.example.cipod_final.ui.theme.SunsetOrange
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable

@Composable
@MaplibreComposable
internal fun PedestrianRouteOverlay(route: PedestrianRouteResult?) {
    val available = route as? PedestrianRouteResult.Available ?: return
    if (available.points.size < 2) return
    val geoJson = remember(available.points) {
        GeoJsonData.JsonString(routeGeoJson(available.points))
    }
    val source = rememberGeoJsonSource(geoJson)
    LineLayer(
        id = "campus-pedestrian-route",
        source = source,
        color = const(SunsetOrange),
        width = const(5.dp)
    )
}

internal fun routeGeoJson(points: List<GeoPoint>): String {
    require(points.size >= 2)
    val positions = points.joinToString(",") { "[${it.longitude},${it.latitude}]" }
    return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$positions]}}]}"""
}
