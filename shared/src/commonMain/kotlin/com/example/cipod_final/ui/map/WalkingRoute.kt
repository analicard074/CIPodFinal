package com.example.cipod_final.ui.map

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.BuildingCoordinates
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.locationNowMillis
import com.example.cipod_final.routing.*
import com.example.cipod_final.ui.theme.ChannelBlue
import kotlinx.coroutines.CancellationException
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable

internal class WalkingRouteState {
    var retry by mutableStateOf(0)
    var failed by mutableStateOf(false)
    var geoJson by mutableStateOf<String?>(null)
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
        val arrival = destination?.let { CampusWalkingAccess.forBuilding(it.id) }
            ?: return@LaunchedEffect
        val origin = when (start) {
            is StartingPoint.Building -> CampusWalkingAccess.forBuilding(start.buildingId)
            is StartingPoint.Device -> {
                if (LocationQualityPolicy.evaluate(start.reading, locationNowMillis()) == LocationQuality.Usable) {
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
        state.geoJson = null
        state.message = null
        try {
            state.geoJson = OrsDirections(client, RoutingConfig.apiKey)
                .walkingRoute(origin, arrival)
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

@Composable
internal fun WalkingRouteControls(state: WalkingRouteState) {
    if (state.busy) Text("Loading walking route…")
    state.message?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
    if (state.failed) {
        TextButton(onClick = { state.retry++ }) { Text("Retry route") }
    }
}

@Composable
@MaplibreComposable
internal fun WalkingRouteLayer(geoJson: String?) {
    if (geoJson == null) return
    val source = rememberGeoJsonSource(GeoJsonData.JsonString(geoJson))
    LineLayer(
        id = "ors-route-layer", source = source,
        color = const(ChannelBlue), width = const(5.dp),
        cap = const(LineCap.Round), join = const(LineJoin.Round),
    )
}
