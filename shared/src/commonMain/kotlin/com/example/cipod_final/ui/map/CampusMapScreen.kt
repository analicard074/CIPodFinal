package com.example.cipod_final.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.RoomDirectoryState
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.rememberLocationController
import com.example.cipod_final.ui.indoor.RoomDirectoryPanel
import com.example.cipod_final.ui.indoor.IndoorFloorViewer
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.AttributionDefaults
import org.maplibre.compose.overlay.AttributionLinks
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.overlay.DisappearingScaleBar
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.overlay.ZoomButtons
import org.maplibre.compose.overlay.attributions
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.Position

@Composable
fun CampusMapScreen(
    navigation: CampusNavigationState,
    locationSession: LocationSession,
    floorEditor: FloorPlanEditorState,
    onNavigationChange: (CampusNavigationState) -> Unit,
    modifier: Modifier = Modifier
) {
    var directory by remember { mutableStateOf<RoomDirectoryState?>(null) }
    var indoorBuildingId by remember { mutableStateOf<String?>(null) }
    var buildingCardHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val controller = rememberLocationController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(locationSession, controller, lifecycle, indoorBuildingId, directory?.buildingId) {
        // Pause refresh when the app is hidden or an indoor floor plan is open.
        if (indoorBuildingId == null && directory == null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                locationSession.refreshAutomatically(controller)
            }
        }
    }
    val walkingRoute = rememberWalkingRoute(
        locationSession.state.start,
        navigation.destination,
        locationSession.readingRevision,
    )
    // Building previews are shared by map markers and Resources search.
    var showBuilding: (CampusBuilding) -> Unit = {}
    val mapState = rememberEphemeralCampusMapState {
        WalkingRouteLayer(walkingRoute.geoJson)
        BuildingMarkers(
            buildings = CampusBuildings.all,
            selectedId = navigation.selectedBuildingId,
            destinationId = navigation.destinationId,
            onBuildingClick = { showBuilding(it) }
        )
        UserLocationMarker(
            reading = locationSession.state.reading,
            expired = locationSession.state.status == LocationStatus.Stale
        )
    }
    val currentLocation = locationSession.state.reading
    val isOutsideCampus = if (currentLocation != null) {
        CampusMapConfig.isOutsideCampus(currentLocation.latitude, currentLocation.longitude)
    } else {
        false
    }

    // Update the view after a location request or when the user enters or leaves campus.
    LaunchedEffect(locationSession.readingRevision, isOutsideCampus) {
        if (currentLocation != null) {
            if (isOutsideCampus) {
                // Show all three buildings instead of moving to the user's off-campus location
                mapState.animateCameraToBounds(
                    CampusMapConfig.buildingBounds,
                    fit = CameraFit(
                        fitPadding = DpPadding(
                            left = 32.dp,
                            top = 128.dp,
                            right = 32.dp,
                            bottom = 32.dp
                        )
                    )
                )
            } else {
                mapState.setCameraPosition(
                    CameraPosition(
                        center = Position(
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude
                        ),
                        zoom = 17.0
                    )
                )
            }
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
    }
    LaunchedEffect(navigation.selectedBuildingId) {
        navigation.selectedBuilding?.coordinates?.let { coordinates ->
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                .heightIn(min = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Explore CI",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        StartingPointControls(
            session = locationSession,
            controller = controller,
            destination = navigation.destination,
            onDestinationChange = { destination ->
                focusManager.clearFocus()
                onNavigationChange(
                    if (destination == null) navigation.clearDestination()
                    else navigation.copy(destinationId = destination.id)
                )
            },
        )
        if (navigation.destination != null) WalkingRouteControls(walkingRoute)
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
                        if (attributions.isNotEmpty() || walkingRoute.geoJson != null) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                shape = MaterialTheme.shapes.extraSmall,
                            ) {
                                Column(Modifier.padding(4.dp)) {
                                    if (attributions.isNotEmpty()) {
                                        AttributionLinks(
                                            attributions = attributions,
                                            textStyle = AttributionDefaults.ContentTextStyle.copy(fontSize = 10.sp),
                                            breakWithinAttribution = true,
                                        )
                                    }
                                    if (walkingRoute.geoJson != null) {
                                        Text(
                                            "© openrouteservice | © OpenStreetMap contributors",
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            end = 8.dp,
                            bottom = 48.dp + if (navigation.selectedBuilding != null) buildingCardHeight else 0.dp,
                        ),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MyLocationMapButton(locationSession, controller)
                        ZoomButtons()
                    }
                }
            }
            LocationAccuracyNotice(
                session = locationSession,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            navigation.selectedBuilding?.let { building ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                ) {
                    Box(Modifier.onSizeChanged { buildingCardHeight = with(density) { it.height.toDp() } }) {
                        BuildingDetailsCard(
                            building = building,
                            navigationActive = walkingRoute.busy || walkingRoute.geoJson != null,
                            onSelectStartingPoint = {
                                focusManager.clearFocus()
                                locationSession.chooseBuilding(building.id)
                            },
                            onDirections = {
                                focusManager.clearFocus()
                                onNavigationChange(navigation.selectDestination(building.id))
                            },
                            onOpenRoomDirectory = {
                                focusManager.clearFocus()
                                locationSession.cancelRequest()
                                directory = RoomDirectoryState(building.id)
                            },
                            onClose = { onNavigationChange(navigation.copy(selectedBuildingId = null)) }
                        )
                    }
                }
            }
        }
    }

    directory?.let { currentDirectory ->
        RoomDirectoryPanel(
            state = currentDirectory,
            onChange = { directory = it },
            onBack = { directory = null },
            backLabel = "Back to map",
            onViewFloorPlan = if (FloorPlanCatalog.forBuilding(currentDirectory.buildingId).isNotEmpty()) ({
                indoorBuildingId = currentDirectory.buildingId
            }) else null
        )
    }
    indoorBuildingId?.let { buildingId ->
        IndoorFloorViewer(buildingId, floorEditor, onBack = { indoorBuildingId = null })
    }
}
