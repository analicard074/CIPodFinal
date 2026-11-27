package com.example.cipod_final.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
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
import com.example.cipod_final.indoor.RoomDirectoryState
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.locationNowMillis
import com.example.cipod_final.location.rememberLocationController
import com.example.cipod_final.routing.CampusWalkingAccess
import com.example.cipod_final.routing.nearEntrance
import com.example.cipod_final.ui.ScreenTitle
import com.example.cipod_final.ui.indoor.IndoorFloorViewer
import com.example.cipod_final.ui.indoor.RoomDirectoryPanel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.AttributionDefaults
import org.maplibre.compose.overlay.AttributionLinks
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.overlay.DisappearingScaleBar
import org.maplibre.compose.overlay.GeographicLayout
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.overlay.ZoomButtons
import org.maplibre.compose.overlay.attributions
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

@Composable
fun CampusMapScreen(
    navigation: CampusNavigationState,
    locationSession: LocationSession,
    floorEditor: FloorPlanEditorState,
    onNavigationChange: (CampusNavigationState) -> Unit,
    modifier: Modifier = Modifier,
    accessibilityEnabled: Boolean = false,
) {
    var directory by remember { mutableStateOf<RoomDirectoryState?>(null) }
    var indoorBuildingId by remember { mutableStateOf<String?>(null) }
    var openingIndoorId by remember { mutableStateOf<String?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var entranceCalloutOpen by remember(navigation.destinationId, navigation.destinationRoomId) { mutableStateOf(false) }
    var arrivalPromptHeight by remember { mutableStateOf(0.dp) }
    var mapNoticeHeight by remember { mutableStateOf(0.dp) }
    var buildingCardHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val controller = rememberLocationController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(locationSession, controller, lifecycle, indoorBuildingId, directory?.buildingId,
        navigation.startRoomId, navigation.destinationRoomId) {
        // Pause refresh when the app is hidden or an indoor floor plan is open.
        if (indoorBuildingId == null && directory == null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                locationSession.refreshAutomatically(controller)
            }
        }
    }
    val walkingRoute = rememberWalkingRoute(
        locationSession.state.start,
        navigation.destination.takeIf { navigation.startRoom == null },
        locationSession.readingRevision,
    )
    val showRouteSummary = walkingRoute.details != null && navigation.startRoom == null
    // Building previews are shared by map markers and Resources search.
    val showBuilding: (CampusBuilding) -> Unit = { building ->
        entranceCalloutOpen = false
        focusManager.clearFocus()
        onNavigationChange(navigation.preview(building.id))
    }
    val entrance = navigation.destination?.let { CampusWalkingAccess.forBuilding(it.id) }
    val startingBuilding = navigation.startRoom?.let { CampusBuildings.find(it.buildingId) }
        ?: (locationSession.state.start as? StartingPoint.Building)?.let { CampusBuildings.find(it.buildingId) }
    val startMarker = startingBuilding?.coordinates?.let { Position(it.longitude, it.latitude) }
        ?: walkingRoute.details?.points?.firstOrNull()
    val currentLocation = locationSession.state.reading
    val atEntrance = nearEntrance(currentLocation, entrance, locationNowMillis())
    var arrivalDismissed by remember(navigation.destinationRoomId, atEntrance) { mutableStateOf(false) }
    val showArrival = navigation.destinationRoom != null && atEntrance && !arrivalDismissed &&
        !entranceCalloutOpen && indoorBuildingId == null
    val mapState = rememberEphemeralCampusMapState {
        WalkingRouteLayer(walkingRoute.geoJson)
        BuildingMarkers(
            buildings = CampusBuildings.all,
            selectedId = navigation.selectedBuildingId,
            destinationId = navigation.destinationId,
            onBuildingClick = { showBuilding(it) }
        )
        RouteEndpointMarkers(
            start = startMarker,
            entrance = entrance?.let { Position(it.longitude, it.latitude) },
            entranceLabel = if (navigation.destinationId == "broome-library") "Main Entrance" else "Entrance",
            pulseEntrance = atEntrance && navigation.destinationRoom != null,
            onEntranceClick = {
                if (navigation.destinationRoom != null) entranceCalloutOpen = true
                else navigation.destination?.let { showBuilding(it) }
            },
        )
        UserLocationMarker(
            reading = locationSession.state.reading,
            expired = locationSession.state.status == LocationStatus.Stale
        )
    }
    LaunchedEffect(entranceCalloutOpen) {
        if (entranceCalloutOpen) entrance?.let { point ->
            mapState.animateCamera(CameraUpdate(center = Position(point.longitude, point.latitude),
                zoom = maxOf(17.5, mapState.cameraPosition.zoom)),
                animation = CameraAnimation.Ease { duration = 250.milliseconds })
        }
    }
    val openIndoor: () -> Unit = {
        navigation.destinationRoom?.let { room ->
            focusManager.clearFocus()
            locationSession.cancelRequest()
            openingIndoorId = room.buildingId
        }
    }
    LaunchedEffect(openingIndoorId, navigation.destinationId, navigation.destinationRoomId) {
        val buildingId = openingIndoorId ?: return@LaunchedEffect
        if (buildingId != navigation.destinationRoom?.buildingId) {
            openingIndoorId = null
            return@LaunchedEffect
        }
        entrance?.let { point ->
            mapState.animateCamera(CameraUpdate(center = Position(point.longitude, point.latitude), zoom = 19.0),
                animation = CameraAnimation.Ease { duration = 450.milliseconds })
        }
        delay(150)
        indoorBuildingId = buildingId
        entranceCalloutOpen = false
        openingIndoorId = null
    }
    val showBuildingCard = navigation.selectedBuilding != null &&
        (navigation.destinationRoom == null || navigation.selectedBuildingId != navigation.destinationId)
    LaunchedEffect(showBuildingCard) { if (!showBuildingCard) buildingCardHeight = 0.dp }
    val isOutsideCampus = if (currentLocation != null) {
        CampusMapConfig.isOutsideCampus(currentLocation.latitude, currentLocation.longitude)
    } else {
        false
    }

    // Update the view after a location request or when the user enters or leaves campus.
    LaunchedEffect(locationSession.readingRevision, isOutsideCampus) {
        if (currentLocation != null && walkingRoute.details == null && !walkingRoute.busy) {
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
        if (walkingRoute.details != null || walkingRoute.busy) return@LaunchedEffect
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
    LaunchedEffect(navigation.selectedBuildingId) {
        if (walkingRoute.details != null || walkingRoute.busy) return@LaunchedEffect
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

    LaunchedEffect(walkingRoute.geoJson, mapSize, buildingCardHeight, indoorBuildingId,
        arrivalPromptHeight, mapNoticeHeight, showRouteSummary) {
        val route = walkingRoute.details ?: return@LaunchedEffect
        if (mapSize.width == 0 || mapSize.height == 0) return@LaunchedEffect
        if (openingIndoorId != null || indoorBuildingId != null || entranceCalloutOpen) return@LaunchedEffect
        val mapHeight = with(density) { mapSize.height.toDp() }
        if (mapHeight < 120.dp) return@LaunchedEffect
        val bounds = route.bounds
        val marker = startMarker
        val visibleBounds = BoundingBox(
            west = minOf(bounds.west, marker?.longitude ?: bounds.west),
            south = minOf(bounds.south, marker?.latitude ?: bounds.south),
            east = maxOf(bounds.east, marker?.longitude ?: bounds.east),
            north = maxOf(bounds.north, marker?.latitude ?: bounds.north),
        )
        val topPadding = if (mapNoticeHeight > 0.dp) 76.dp + mapNoticeHeight else 36.dp
        val bottomPadding = (if (showRouteSummary) 80.dp else 64.dp) + maxOf(buildingCardHeight, arrivalPromptHeight)
        val padding = DpPadding(
            left = 28.dp,
            top = topPadding.coerceAtMost(mapHeight * .4f),
            right = 72.dp,
            // The map viewport already excludes the bottom card; leave extra clearance above it.
            bottom = bottomPadding.coerceAtMost(mapHeight * .4f),
        )
        mapState.animateCameraToBounds(visibleBounds, fit = CameraFit(fitPadding = padding))
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                ScreenTitle("Explore CI")
            }
            StartingPointControls(
                session = locationSession,
                controller = controller,
                destination = navigation.destination,
                destinationRoomNumber = navigation.destinationRoom?.number,
                startRoomNumber = navigation.startRoom?.number,
                onClearRoomStart = { onNavigationChange(navigation.clearRoomStart()) },
                onDestinationChange = { destination ->
                    focusManager.clearFocus()
                    onNavigationChange(
                        if (destination == null) navigation.clearDestination()
                        else navigation.selectDestination(destination.id)
                    )
                },
            )
            navigation.destinationContext?.let { resourceName ->
                Text(
                    text = "Resource destination: $resourceName",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            navigation.destinationRoom?.let { room ->
                val floor = if (room.floorPlanId?.contains("second") == true) 2 else 1
                Text(
                    text = "Indoor destination: Room ${room.number} · Floor $floor",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = openIndoor,
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Open indoor directions") }
            }
            if (navigation.destination != null && navigation.startRoom == null) WalkingRouteControls(walkingRoute)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .onSizeChanged { mapSize = it },
            ) {
                MaplibreMap(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = mapState,
                    cameraConstraints = if (walkingRoute.details != null) CameraConstraints { minZoom = 3.0 }
                        else CampusMapConfig.cameraConstraints,
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
                                                walkingRoute.details?.attribution.orEmpty(),
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
                                bottom = 56.dp + maxOf(buildingCardHeight, arrivalPromptHeight) + 8.dp,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd).width(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MyLocationMapButton(locationSession, controller,
                                onSelectLocation = { onNavigationChange(navigation.clearRoomStart()) })
                            ZoomButtons(width = 48.dp)
                        }
                    }
                    navigation.destinationRoom?.let { room ->
                        entrance?.let { point ->
                            GeographicLayout(Modifier.fillMaxSize()) {
                                val position = Position(point.longitude, point.latitude)
                                Box(Modifier.placedAt(position).size(48.dp)
                                    .semantics { contentDescription = "Main Entrance. Indoor directions to room ${room.number}" }
                                    .clickable(role = Role.Button) { entranceCalloutOpen = true })
                                if (entranceCalloutOpen) {
                                    EntranceIndoorCallout(room, navigation.destination?.name.orEmpty(),
                                        opening = openingIndoorId != null, onOpen = openIndoor,
                                        onDismiss = { entranceCalloutOpen = false },
                                        modifier = Modifier.placedAt(position, Alignment.BottomCenter).offset(y = (-30).dp))
                                }
                            }
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showArrival,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 56.dp),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    navigation.destinationRoom?.let { room ->
                        DisposableEffect(room.id) { onDispose { arrivalPromptHeight = 0.dp } }
                        IndoorArrivalPrompt(room, openingIndoorId != null, onOpen = openIndoor,
                            onDismiss = { arrivalDismissed = true },
                            modifier = Modifier.fillMaxWidth().onSizeChanged {
                                arrivalPromptHeight = with(density) { it.height.toDp() }
                            })
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 64.dp)
                        .onSizeChanged { mapNoticeHeight = with(density) { it.height.toDp() } },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (navigation.startRoom == null) {
                        LocationAccuracyNotice(session = locationSession)
                    }
                }
                navigation.selectedBuilding?.takeIf { showBuildingCard }?.let { building ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 48.dp),
                    ) {
                        Box(Modifier.heightIn(max = with(density) { (mapSize.height * .38f).toDp() }.coerceAtLeast(1.dp))
                            .onSizeChanged { buildingCardHeight = with(density) { it.height.toDp() } }) {
                            BuildingDetailsCard(
                                building = building,
                                navigationActive = walkingRoute.busy || walkingRoute.geoJson != null,
                                onSelectStartingPoint = {
                                    focusManager.clearFocus()
                                    locationSession.chooseBuilding(building.id)
                                    onNavigationChange(navigation.clearRoomStart())
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
            if (showRouteSummary) walkingRoute.details?.let { route ->
                RouteSummaryCard(
                    route = route,
                    onEndRoute = {
                        focusManager.clearFocus()
                        entranceCalloutOpen = false
                        openingIndoorId = null
                        onNavigationChange(navigation.clearDestination())
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    // The parent Scaffold reserves the bottom navigation and home-indicator inset.
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                )
            }
        }

        directory?.takeIf { indoorBuildingId == null }?.let { currentDirectory ->
            RoomDirectoryPanel(
                state = currentDirectory,
                onChange = { directory = it },
                onBack = { directory = null },
                backLabel = "Back to map",
                startRoomId = navigation.startRoomId,
                onStartFromRoom = { room ->
                    onNavigationChange(navigation.selectRoomStart(room))
                },
                onViewFloorPlan = if (FloorPlanCatalog.forBuilding(currentDirectory.buildingId).isNotEmpty()) ({
                    indoorBuildingId = currentDirectory.buildingId
                }) else null,
                onDirectionsToRoom = { room ->
                    onNavigationChange(navigation.selectRoomDestination(room))
                    directory = null
                    indoorBuildingId = room.buildingId.takeIf { navigation.startRoom?.buildingId == it }
                },
            )
        }
        AnimatedVisibility(visible = indoorBuildingId != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(180))) {
            val buildingId = indoorBuildingId ?: navigation.destinationId ?: return@AnimatedVisibility
            IndoorFloorViewer(
                buildingId, floorEditor,
                onBack = { indoorBuildingId = null },
                showOutdoorBackLink = directory == null,
                initialRoomId = navigation.destinationRoom?.takeIf { it.buildingId == buildingId }?.id
                    ?: directory?.selectedRoomId,
                routingToRoom = navigation.destinationRoom?.buildingId == buildingId,
                accessibilityEnabled = accessibilityEnabled,
                startRoomId = navigation.startRoom?.takeIf { it.buildingId == buildingId }?.id,
            )
        }
    }
}
