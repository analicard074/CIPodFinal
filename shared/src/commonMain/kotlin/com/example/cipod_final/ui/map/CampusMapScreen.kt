package com.example.cipod_final.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.rememberLocationController
import com.example.cipod_final.ui.indoor.IndoorFloorViewer
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.AttributionDefaults
import org.maplibre.compose.overlay.AttributionLinks
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.overlay.DisappearingScaleBar
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.overlay.ZoomButtons
import org.maplibre.compose.overlay.attributions
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
    var searchResultsVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val controller = rememberLocationController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(locationSession, controller, lifecycle, indoorBuildingId) {
        // Pause refresh when the app is hidden or an indoor floor plan is open.
        if (indoorBuildingId == null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                locationSession.refreshAutomatically(controller)
            }
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
    LaunchedEffect(locationSession.readingRevision) {
        locationSession.state.reading?.let { reading ->
            mapState.setCameraPosition(
                CameraPosition(
                    center = Position(latitude = reading.latitude, longitude = reading.longitude),
                    zoom = 17.0
                )
            )
        }
    }
    LaunchedEffect(locationSession.state.start) {
        val start = locationSession.state.start as? StartingPoint.Building
        start?.let { CampusBuildings.find(it.buildingId)?.coordinates }?.let { coordinates ->
            mapState.setCameraPosition(
                CameraPosition(
                    center = Position(
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude
                    ), zoom = 17.0
                )
            )
        }
    }
    showBuilding = { building ->
        focusManager.clearFocus()
        onNavigationChange(navigation.preview(building.id))
        building.coordinates?.let { coordinates ->
            mapState.setCameraPosition(
                CameraPosition(
                    center = Position(
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude
                    ),
                    zoom = 17.0
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        BuildingSearchBar(
            query = navigation.query,
            results = navigation.results,
            onQueryChange = { onNavigationChange(navigation.copy(query = it)) },
            onBuildingClick = showBuilding,
            onResultsVisibilityChange = { searchResultsVisible = it },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
                .clip(MaterialTheme.shapes.medium),
        ) {
            MaplibreMap(
                modifier = Modifier
                    .fillMaxSize(),
                state = mapState,
                cameraConstraints = CampusMapConfig.cameraConstraints,
            ) {
                // Keep source attribution and map controls inside the unobscured map viewport.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
                {
                    DisappearingScaleBar(
                        metersPerDp = { mapState.viewport?.metersPerDpAtCenter ?: 0.0 },
                        zoom = { mapState.cameraPosition.zoom },
                        modifier = Modifier
                            .align(Alignment.TopStart),
                    )
                    DisappearingCompassButton(modifier = Modifier.align(Alignment.TopEnd))
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        MaplibreLogo()
                        val attributions = mapState.style.attributions()
                        if (attributions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                shape = MaterialTheme.shapes.extraSmall,
                            ) {
                                AttributionLinks(
                                    attributions = attributions,
                                    textStyle = AttributionDefaults.ContentTextStyle.copy(fontSize = 10.sp),
                                    modifier = Modifier.padding(4.dp),
                                    breakWithinAttribution = true,
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 8.dp, bottom = 48.dp),
                ) {
                    ZoomButtons(modifier = Modifier.align(Alignment.BottomEnd))
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                if (!searchResultsVisible) {
                    StartingPointControls(locationSession, controller)
                }
                navigation.destination?.takeUnless { searchResultsVisible }?.let { destination ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                        {
                            TextButton(
                                onClick = { showBuilding(destination) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Destination: ${destination.name}")
                            }
                            TextButton(onClick = { onNavigationChange(navigation.clearDestination()) }) {
                                Text("Clear destination")
                            }
                        }
                    }
                }
            }
        }
        navigation.selectedBuilding?.let { building ->
            BuildingDetailsCard(
                building = building,
                destination = navigation.destination,
                onSelectDestination = {
                    focusManager.clearFocus()
                    onNavigationChange(navigation.selectDestination(building.id))
                    building.coordinates?.let { coordinates ->
                        mapState.setCameraPosition(
                            CameraPosition(
                                center = Position(
                                    latitude = coordinates.latitude,
                                    longitude = coordinates.longitude
                                ),
                                zoom = 17.0
                            )
                        )
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
