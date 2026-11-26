package com.example.cipod_final.ui.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.ui.theme.ChannelBlue
import com.example.cipod_final.ui.theme.WarmCream
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position

@Composable
@MaplibreComposable
internal fun BuildingMarkers(
    buildings: List<CampusBuilding>,
    selectedId: String?,
    destinationId: String?,
    onBuildingClick: (CampusBuilding) -> Unit
) {
    buildings.forEach { building ->
        val coordinates = building.coordinates ?: return@forEach
        key(building.id) {
            val data = remember(building) {
                GeoJsonData.JsonString(
                    """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${coordinates.longitude},${coordinates.latitude}]}}]}"""
                )
            }
            val source = rememberGeoJsonSource(data)
            SymbolLayer(
                id = "building-label-${building.id}",
                source = source,
                textField = format(span(building.name)),
                textSize = const(13.sp),
                textFont = const(listOf("Noto Sans Regular")),
                textColor = const(ChannelBlue),
                textHaloColor = const(WarmCream),
                textHaloWidth = const(2.dp),
                textAnchor = const(SymbolAnchor.Bottom),
                textRadialOffset = const(1.5.em),
                onClick = {
                    onBuildingClick(building)
                    ClickResult.Consume
                }
            )
            CircleLayer(
                id = "building-${building.id}",
                source = source,
                radius = const(if (building.id == destinationId) 13.dp else 10.dp),
                color = const(ChannelBlue),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp),
                hitPadding = 14.dp,
                onClick = {
                    onBuildingClick(building)
                    ClickResult.Consume
                }
            )
        }
    }
}

@Composable
@MaplibreComposable
internal fun RouteEndpointMarkers(
    start: Position?,
    entrance: Position?,
    entranceLabel: String,
    pulseEntrance: Boolean = false,
    onEntranceClick: () -> Unit,
) {
    if (start != null) EndpointMarker("start", start, "Start", false, {})
    if (entrance != null) EndpointMarker("entrance", entrance, entranceLabel, true, onEntranceClick, pulseEntrance)
}

@Composable
@MaplibreComposable
private fun EndpointMarker(
    id: String,
    point: Position,
    label: String,
    arrival: Boolean,
    onClick: () -> Unit,
    pulse: Boolean = false,
) {
    val haloRadius = if (arrival && pulse) {
        val transition = rememberInfiniteTransition(label = "Entrance pulse")
        val radius by transition.animateFloat(19f, 25f,
            infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "Entrance halo")
        radius.dp
    } else if (arrival) 19.dp else 16.dp
    val source = rememberGeoJsonSource(GeoJsonData.JsonString(
        """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]}}"""
    ))
    CircleLayer(id = "route-$id-halo", source = source, radius = const(haloRadius),
        color = const(ChannelBlue.copy(alpha = .18f)))
    CircleLayer(id = "route-$id-marker", source = source, radius = const(if (arrival) 12.dp else 8.dp),
        color = const(if (arrival) ChannelBlue else Color.White),
        strokeColor = const(if (arrival) Color.White else ChannelBlue), strokeWidth = const(3.dp),
        hitPadding = 14.dp, onClick = {
            onClick()
            ClickResult.Consume
        })
    SymbolLayer(id = "route-$id-label", source = source,
        textField = format(span(label)), textFont = const(listOf("Noto Sans Regular")),
        textColor = const(ChannelBlue), textHaloColor = const(Color.White), textHaloWidth = const(2.dp),
        textSize = const(13.sp), textAnchor = const(SymbolAnchor.Bottom), textRadialOffset = const(1.6.em),
        onClick = {
            onClick()
            ClickResult.Consume
        })
}
