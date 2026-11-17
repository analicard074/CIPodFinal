package com.example.cipod_final.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.location.LocationController
import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.locationNowMillis
import kotlin.math.roundToInt

@Composable
internal fun StartingPointControls(session: LocationSession, controller: LocationController) {
    val state = session.state
    val focusManager = LocalFocusManager.current
    var chooseBuilding by remember { mutableStateOf(false) }
    val quality = state.reading?.let { LocationQualityPolicy.evaluate(it, locationNowMillis()) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().heightIn(max = 160.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(enabled = !state.busy, onClick = {
                    focusManager.clearFocus()
                    session.requestLocation(controller)
                }) {
                    Text(if (state.status == LocationStatus.Idle) "Use My Location" else "Retry location")
                }
                TextButton(onClick = {
                    focusManager.clearFocus()
                    session.cancelRequest()
                    chooseBuilding = true
                }) { Text("Choose a building") }
                if (state.busy) TextButton(onClick = session::cancelRequest) { Text("Cancel") }
            }
            val message = when (state.status) {
                LocationStatus.Idle -> "Choose a starting point. Your destination stays separate."
                LocationStatus.RequestingPermission -> "Waiting for foreground location permission…"
                LocationStatus.Locating -> "Getting one location reading…"
                LocationStatus.Ready -> {
                    val accuracy = state.reading?.accuracyMeters?.roundToInt()
                    if (quality == LocationQuality.Usable) "One-time snapshot • accuracy about $accuracy m • expires after 30 seconds."
                    else "Approximate location (about $accuracy m). Retry or choose a building."
                }
                LocationStatus.Denied -> "Location permission denied. Choose a building or enable access in app settings."
                LocationStatus.ServicesDisabled -> "Location services are off. Enable them in Settings, then retry, or choose a building."
                LocationStatus.Unavailable -> "No location fix available. Try outdoors, retry, or choose a building."
                LocationStatus.TimedOut -> "Location request timed out. Retry or choose a building."
                LocationStatus.Cancelled -> "Location request cancelled. Retry or choose a building."
                LocationStatus.Stale -> "This location snapshot has expired. Retry or choose a building."
            }
            Text(message, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (state.status == LocationStatus.Ready && state.start !is StartingPoint.Device) {
                Button(onClick = session::useReading, enabled = quality == LocationQuality.Usable) {
                    Text("Use location as starting point")
                }
            }
            if (state.status == LocationStatus.Denied || state.status == LocationStatus.ServicesDisabled ||
                quality == LocationQuality.Approximate) {
                TextButton(onClick = {
                    session.cancelRequest()
                    if (state.status == LocationStatus.ServicesDisabled) controller.openLocationSettings()
                    else controller.openAppSettings()
                }) { Text("Open Settings") }
            }
            state.start?.let { start ->
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    val name = when (start) {
                        is StartingPoint.Building -> CampusBuildings.find(start.buildingId)?.name.orEmpty()
                        is StartingPoint.Device -> "My location (snapshot)"
                    }
                    TextButton(onClick = session::clear) { Text("Clear starting point: $name") }
                }
            }
        }
    }
    if (chooseBuilding) {
        AlertDialog(
            onDismissRequest = { chooseBuilding = false },
            title = { Text("Choose a starting building") },
            text = {
                Column {
                    Text("Only buildings with verified coordinates are available.")
                    CampusBuildings.all.filter { it.coordinates != null }.forEach { building ->
                        TextButton(onClick = {
                            session.chooseBuilding(building.id)
                            chooseBuilding = false
                        }) { Text(building.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { chooseBuilding = false }) { Text("Close") } }
        )
    }
}
