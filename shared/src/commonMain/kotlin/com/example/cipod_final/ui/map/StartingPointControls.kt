package com.example.cipod_final.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.location.LocationController
import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.locationNowMillis
import com.example.cipod_final.routing.CampusWalkingAccess

@Composable
internal fun StartingPointControls(
    session: LocationSession,
    controller: LocationController,
    destination: CampusBuilding?,
    onDestinationChange: (CampusBuilding?) -> Unit,
    destinationRoomNumber: String? = null,
    startRoomNumber: String? = null,
    onClearRoomStart: () -> Unit = {},
) {
    val state = session.state
    val showLocationFeedback = (session.locationFeedbackRequested || state.start == null) && startRoomNumber == null
    val showLocationProgress = showLocationFeedback && state.busy &&
        (session.isRequestingLocation || state.start == null)
    val locationFailed = when (state.status) {
        LocationStatus.Denied, LocationStatus.ServicesDisabled, LocationStatus.Unavailable,
        LocationStatus.TimedOut, LocationStatus.Stale -> true
        LocationStatus.Ready -> state.reading?.let {
            LocationQualityPolicy.evaluate(it, locationNowMillis()) != LocationQuality.Usable
        } == true
        else -> false
    }
    val needsBuilding = showLocationFeedback && state.start == null && !state.busy && locationFailed
    val destinationName = when {
        destination == null -> null
        destinationRoomNumber == null -> destination.name
        destination.id == "broome-library" -> "Broome $destinationRoomNumber"
        else -> "${destination.name} $destinationRoomNumber"
    }
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteEndpointRow(
                modifier = Modifier.weight(1f),
                label = "From",
                requestBuildingSelection = needsBuilding,
                name = startRoomNumber?.let { "Broome $it" } ?: when (val start = state.start) {
                    is StartingPoint.Building -> CampusBuildings.find(start.buildingId)?.name
                    is StartingPoint.Device -> "My location"
                    null -> null
                },
                onClear = {
                    session.clear()
                    onClearRoomStart()
                },
                onSelectBuilding = {
                    onClearRoomStart()
                    session.chooseBuilding(it.id)
                },
                onSelectLocation = {
                    focusManager.clearFocus()
                    onClearRoomStart()
                    session.selectCurrentLocation(controller)
                },
            )
            Text(
                "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RouteEndpointRow(
                modifier = Modifier.weight(1f),
                label = "To",
                name = destinationName,
                onClear = { onDestinationChange(null) },
                onSelectBuilding = { onDestinationChange(it) },
            )
        }
        if (showLocationProgress) {
            TextButton(onClick = session::cancelRequest) { Text("Cancel") }
        }
        val message = if (!showLocationFeedback) null else when (state.status) {
            LocationStatus.Idle -> session.fallbackBuildingId?.let { id ->
                CampusBuildings.find(id)?.let { "Location signal is weak. Starting from ${it.name}. You can change the From building." }
            }
            LocationStatus.RequestingPermission -> if (showLocationProgress) "Waiting for foreground location permission..." else null
            LocationStatus.Locating -> if (showLocationProgress) "Getting location..." else null
            LocationStatus.Ready -> {
                val currentLocation = state.reading
                when {
                    currentLocation != null &&
                        CampusMapConfig.isOutsideCampus(currentLocation.latitude, currentLocation.longitude) ->
                        "You are outside of campus."
                    needsBuilding -> "Location signal is weak. Choose your starting building."
                    else -> null
                }
            }
            LocationStatus.Denied -> "Location permission is off. Tap My location to request access."
            LocationStatus.ServicesDisabled -> "Location services are off. Enable them in Settings, then retry, or choose a building."
            LocationStatus.Unavailable -> "No location fix available. Try outdoors, retry, or choose a building."
            LocationStatus.TimedOut -> "Location request timed out. Retry or choose a building."
            LocationStatus.Cancelled -> "Location request cancelled. Retry or choose a building."
            LocationStatus.Stale -> "This location snapshot has expired. Retry or choose a building."
        }
        if (message != null) {
            Surface(
                modifier = Modifier.padding(top = 4.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun RouteEndpointRow(
    label: String,
    name: String?,
    onClear: () -> Unit,
    onSelectBuilding: (CampusBuilding) -> Unit,
    onSelectLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    requestBuildingSelection: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var promptedForBuilding by remember(name) { mutableStateOf(false) }
    LaunchedEffect(requestBuildingSelection) {
        if (requestBuildingSelection && !promptedForBuilding) {
            expanded = true
            promptedForBuilding = true
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = mapSearchFieldColor(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            name ?: if (onSelectLocation != null) "Choose starting point" else "Choose destination",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    onSelectLocation?.let { selectLocation ->
                        DropdownMenuItem(
                            text = { Text("My location") },
                            onClick = {
                                expanded = false
                                selectLocation()
                            },
                        )
                    }
                    CampusBuildings.all.filter { CampusWalkingAccess.supportsBuilding(it.id) }.forEach { building ->
                        DropdownMenuItem(
                            text = { Text(building.name) },
                            onClick = {
                                expanded = false
                                onSelectBuilding(building)
                            },
                        )
                    }
                }
            }
            if (name != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
                        contentDescription = if (onSelectLocation != null) "Clear starting point" else "Clear destination"
                    },
                ) {
                    Text("×", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
internal fun MyLocationMapButton(session: LocationSession, controller: LocationController, onSelectLocation: () -> Unit = {}) {
    val focusManager = LocalFocusManager.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        IconButton(
            onClick = {
                focusManager.clearFocus()
                onSelectLocation()
                session.selectCurrentLocation(controller)
            },
            enabled = !session.isRequestingLocation,
            modifier = Modifier.size(48.dp).semantics { contentDescription = "My location" },
        ) {
            val iconColor = MaterialTheme.colorScheme.primary
            if (session.isRequestingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Canvas(Modifier.size(24.dp)) {
                    val radius = size.minDimension * 0.3f
                    drawCircle(iconColor, radius, style = Stroke(1.5.dp.toPx()))
                    drawCircle(iconColor, radius * 0.35f)
                    val edge = size.minDimension / 2
                    for (direction in listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f))) {
                        drawLine(iconColor, center + direction * radius, center + direction * edge, 1.5.dp.toPx())
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocationAccuracyNotice(session: LocationSession, modifier: Modifier = Modifier) {
    val state = session.state
    val reading = state.reading
    val quality = reading?.let { LocationQualityPolicy.evaluate(it, locationNowMillis()) }
    // Keep the notice dismissed across GPS updates until accuracy recovers.
    var dismissed by remember(quality == LocationQuality.Usable) { mutableStateOf(false) }
    if (!session.locationFeedbackRequested || reading == null || state.status != LocationStatus.Ready ||
        quality != LocationQuality.Approximate || dismissed
    ) return

    Surface(
        modifier = modifier.padding(horizontal = 48.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color(0xFFF1F1F1),
        contentColor = Color(0xFF333333),
        shadowElevation = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (!reading.precisePermission) {
                    "Enable Precise Location in app settings to use your location as the starting point."
                } else {
                    "Location accuracy is low. Move to an open area for a stronger signal."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            IconButton(
                onClick = { dismissed = true },
                modifier = Modifier.semantics { contentDescription = "Dismiss location accuracy message" },
            ) {
                Text("×", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
