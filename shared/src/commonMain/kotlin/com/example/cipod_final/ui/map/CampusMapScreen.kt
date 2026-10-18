package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.buildings.CampusNavigationState
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.spatialk.geojson.Position

@Composable
fun CampusMapScreen(
    navigation: CampusNavigationState,
    onNavigationChange: (CampusNavigationState) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    // One event path for markers, search results, and the destination chip.
    var showBuilding: (CampusBuilding) -> Unit = {}
    val mapState = rememberMapState(
        baseStyle = CampusMapConfig.baseStyle,
        initialCameraPosition = CampusMapConfig.initialCameraPosition
    ) {
        BuildingMarkers(
            buildings = navigation.results,
            selectedId = navigation.selectedBuildingId,
            destinationId = navigation.destinationId,
            onBuildingClick = { showBuilding(it) }
        )
    }
    showBuilding = { building ->
        focusManager.clearFocus()
        onNavigationChange(navigation.preview(building.id))
        building.coordinates?.let { coordinates ->
            mapState.setCameraPosition(CameraPosition(
                center = Position(latitude = coordinates.latitude, longitude = coordinates.longitude),
                zoom = 17.0
            ))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BuildingSearchBar(
            query = navigation.query,
            results = navigation.results,
            onQueryChange = { onNavigationChange(navigation.copy(query = it)) },
            onBuildingClick = showBuilding
        )
        navigation.destination?.let { destination ->
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    TextButton(onClick = { showBuilding(destination) }, modifier = Modifier.weight(1f)) {
                        Text("Destination: ${destination.name}")
                    }
                    TextButton(onClick = { onNavigationChange(navigation.clearDestination()) }) {
                        Text("Clear destination")
                    }
                }
            }
        }
        MaplibreMap(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = mapState
        ) {
            // Keep source attribution and map controls inside the unobscured map viewport.
            include(MapOverlay.Full)
        }
        navigation.selectedBuilding?.let { building ->
            BuildingDetailsCard(
                building = building,
                destination = navigation.destination,
                onSelectDestination = {
                    focusManager.clearFocus()
                    onNavigationChange(navigation.selectDestination(building.id))
                    building.coordinates?.let { coordinates ->
                        mapState.setCameraPosition(CameraPosition(
                            center = Position(latitude = coordinates.latitude, longitude = coordinates.longitude),
                            zoom = 17.0
                        ))
                    }
                },
                onClose = { onNavigationChange(navigation.copy(selectedBuildingId = null)) }
            )
        }
    }
}
