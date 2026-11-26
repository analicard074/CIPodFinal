package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.BuildingCoordinates
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.indoor.DirectoryRoom
import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.locationNowMillis
import com.example.cipod_final.routing.CampusWalkingAccess
import com.example.cipod_final.routing.OrsDirections
import com.example.cipod_final.routing.RouteFailure
import com.example.cipod_final.routing.WalkingRouteDetails
import com.example.cipod_final.routing.routingHttpClient
import com.example.cipod_final.routing.walkingRouteDetails
import com.example.cipod_final.ui.theme.ChannelBlue
import kotlinx.coroutines.CancellationException
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable

internal class WalkingRouteState {
    var retry by mutableStateOf(0)
    var failed by mutableStateOf(false)
    var details by mutableStateOf<WalkingRouteDetails?>(null)
    val geoJson: String? get() = details?.displayGeoJson
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
}

@Composable
internal fun rememberWalkingRoute(
    start: StartingPoint?,
    destination: CampusBuilding?,
    locationRevision: Long = 0L,
): WalkingRouteState {
    // Route on destination/start selection, without issuing a request for each automatic GPS fix.
    val startIdentity = when (start) {
        is StartingPoint.Building -> start.buildingId
        is StartingPoint.Device -> "device"
        null -> null
    }
    // Explicit location refreshes should retry routing; automatic GPS fixes should not.
    val originRevision = if (start is StartingPoint.Device) locationRevision else 0L
    val state = remember(startIdentity, destination?.id, originRevision) { WalkingRouteState() }
    val client = remember { routingHttpClient() }
    DisposableEffect(client) { onDispose { client.close() } }
    LaunchedEffect(state, state.retry) {
        val target = destination ?: return@LaunchedEffect
        val arrival = CampusWalkingAccess.forBuilding(target.id) ?: return@LaunchedEffect
        val origin = when (start) {
            is StartingPoint.Building -> CampusWalkingAccess.departureFor(
                start.buildingId,
                target.id
            )

            is StartingPoint.Device -> {
                if (LocationQualityPolicy.evaluate(
                        start.reading,
                        locationNowMillis()
                    ) == LocationQuality.Usable
                ) {
                    BuildingCoordinates(start.reading.latitude, start.reading.longitude)
                } else null
            }

            null -> null
        }
        if (origin == null) {
            state.message = "Choose a starting building or refresh your location."
            return@LaunchedEffect
        }
        state.busy = true
        state.failed = false
        state.details = null
        state.message = null
        try {
            val response = OrsDirections(client, RoutingConfig.apiKey).walkingRoute(origin, arrival)
            state.details = walkingRouteDetails(response, origin, arrival).withDestination(target)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: RouteFailure) {
            state.failed = true
            state.message = failure.userMessage
        } catch (_: Exception) {
            state.failed = true
            state.message = "Could not load route. Check your connection and retry."
        } finally {
            state.busy = false
        }
    }
    return state
}

private fun WalkingRouteDetails.withDestination(destination: CampusBuilding): WalkingRouteDetails =
    copy(
        destinationName = destination.name,
        entranceLabel = if (destination.id == "broome-library") "Main Entrance" else "Entrance",
    )

@Composable
internal fun WalkingRouteControls(state: WalkingRouteState) {
    if (state.busy) Text("Loading walking route…")
    state.message?.let {
        Text(
            it,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
    if (state.failed) {
        TextButton(onClick = { state.retry++ }) { Text("Retry route") }
    }
}

@Composable
@MaplibreComposable
internal fun WalkingRouteLayer(geoJson: String?) {
    if (geoJson == null) return
    val source = rememberGeoJsonSource(GeoJsonData.JsonString(geoJson))
    // Place the line below street labels so their existing light halos remain visible.
    Anchor.Below(predicate = { it.type == "symbol" }) {
        LineLayer(
            id = "ors-route-halo", source = source,
            color = const(Color.White), width = const(9.dp),
            cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
        LineLayer(
            id = "ors-route-layer", source = source,
            color = const(ChannelBlue), width = const(5.dp),
            cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
    }
}

@Composable
internal fun EntranceIndoorCallout(
    room: DirectoryRoom,
    buildingName: String,
    opening: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.width(240.dp), shape = MaterialTheme.shapes.medium, shadowElevation = 8.dp) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(buildingName, style = MaterialTheme.typography.titleMedium)
                    Text(roomFloorLabel(room), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).semantics {
                    contentDescription = "Close entrance details"
                }) { Text("×") }
            }
            TextButton(
                onClick = onOpen,
                enabled = !opening,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    if (opening) "Opening indoor map…" else "Indoor directions →",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

@Composable
internal fun IndoorArrivalPrompt(
    room: DirectoryRoom,
    opening: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "You've arrived", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(roomFloorLabel(room), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                onClick = onOpen, enabled = !opening, modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    if (opening) "Opening indoor map…" else "Start indoor directions →",
                    textAlign = TextAlign.Center
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).semantics {
                contentDescription = "Dismiss arrival prompt"
            }) { Text("×") }
        }
    }
}

private fun roomFloorLabel(room: DirectoryRoom): String {
    val floor = if (room.floorPlanId?.contains("second") == true) 2 else 1
    return "Room ${room.number} · Floor $floor"
}
