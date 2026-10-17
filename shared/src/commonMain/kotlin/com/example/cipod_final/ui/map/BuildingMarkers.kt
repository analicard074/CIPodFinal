package com.example.cipod_final.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.ui.theme.ChannelBlue
import com.example.cipod_final.ui.theme.Seafoam
import com.example.cipod_final.ui.theme.SunsetOrange
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
                color = const(when (building.id) {
                    destinationId -> SunsetOrange
                    selectedId -> Seafoam
                    else -> ChannelBlue
                }),
                strokeColor = const(WarmCream),
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
