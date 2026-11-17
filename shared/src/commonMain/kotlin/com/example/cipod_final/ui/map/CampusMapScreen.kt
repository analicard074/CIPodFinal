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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.ui.indoor.IndoorFloorViewer
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.rememberLocationController
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.spatialk.geojson.Position

@Composable
fun CampusMapScreen(
    navigation: CampusNavigationState,
    locationSession: LocationSession,
    floorEditor: FloorPlanEditorState,
    onNavigationChange: (CampusNavigationState) -> Unit,
    modifier: Modifier = Modifier
) {
    var indoorBuildingId by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val controller = rememberLocationController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(locationSession, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) locationSession.cancelRequest()
            if (event == Lifecycle.Event.ON_START) locationSession.refreshFreshness()
        }
        lifecycle.addObserver(observer)
        locationSession.refreshFreshness()
        onDispose {
            lifecycle.removeObserver(observer)
            locationSession.cancelRequest()
        }
    }
    // One event path for markers, search results, and the destination chip.
    var showBuilding: (CampusBuilding) -> Unit = {}
    val mapState = rememberEphemeralCampusMapState {
        BuildingMarkers(
            buildings = navigation.results,
            selectedId = navigation.selectedBuildingId,
            destinationId = navigation.destinationId,
            onBuildingClick = { showBuilding(it) }
        )
        UserLocationMarker(
            reading = locationSession.state.reading,
            expired = locationSession.state.status == LocationStatus.Stale
        )
    }
    LaunchedEffect(locationSession.readingRevision, locationSession.state.reading) {
        locationSession.state.reading?.let { reading ->
            mapState.setCameraPosition(CameraPosition(
                center = Position(latitude = reading.latitude, longitude = reading.longitude), zoom = 17.0
            ))
        }
    }
    LaunchedEffect(locationSession.state.start) {
        val start = locationSession.state.start as? StartingPoint.Building
        start?.let { CampusBuildings.find(it.buildingId)?.coordinates }?.let { coordinates ->
            mapState.setCameraPosition(CameraPosition(
                center = Position(latitude = coordinates.latitude, longitude = coordinates.longitude), zoom = 17.0
            ))
        }
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
            onBuildingClick = showBuilding,
            maxResultsHeight = 96.dp
        )
        StartingPointControls(locationSession, controller)
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
                onSelectStartingPoint = {
                    focusManager.clearFocus()
                    locationSession.chooseBuilding(building.id)
                },
                onViewFloorPlans = if (FloorPlanCatalog.forBuilding(building.id).isNotEmpty()) ({
                    focusManager.clearFocus()
                    locationSession.cancelRequest()
                    indoorBuildingId = building.id
                }) else null,
                onClose = { onNavigationChange(navigation.copy(selectedBuildingId = null)) }
            )
        }
    }
    indoorBuildingId?.let { buildingId ->
        IndoorFloorViewer(buildingId, floorEditor, onBack = { indoorBuildingId = null })
    }
}
