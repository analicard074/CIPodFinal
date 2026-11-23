package com.example.cipod_final.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.location.LocationController
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint

@Composable
internal fun StartingPointControls(session: LocationSession, controller: LocationController) {
    val state = session.state
    val focusManager = LocalFocusManager.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
            ) {
                ElevatedButton(
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
                    enabled = !state.busy,
                    onClick = {
                        focusManager.clearFocus()
                        session.requestLocation(controller)
                    },
                ) {
                    Text(
                        when (state.status) {
                            LocationStatus.Denied -> "Use Location"
                            LocationStatus.Idle -> "Use My Location"
                            else -> "Refresh location"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (state.busy) TextButton(onClick = session::cancelRequest) { Text("Cancel") }
            }
            val message = when (state.status) {
                LocationStatus.Idle -> "Choose your destination or desired building."
                LocationStatus.RequestingPermission -> "Waiting for foreground location permission..."
                LocationStatus.Locating -> "Getting location..."
                LocationStatus.Ready -> {
                    val currentLocation = state.reading
                    if (currentLocation != null &&
                        CampusMapConfig.isOutsideCampus(currentLocation.latitude, currentLocation.longitude)
                    ) {
                        "You are outside of campus."
                    } else {
                        null
                    }
                }
                LocationStatus.Denied -> "Location permission is off. Tap Use Location to request access."
                LocationStatus.ServicesDisabled -> "Location services are off. Enable them in Settings, then retry, or choose a building."
                LocationStatus.Unavailable -> "No location fix available. Try outdoors, retry, or choose a building."
                LocationStatus.TimedOut -> "Location request timed out. Retry or choose a building."
                LocationStatus.Cancelled -> "Location request cancelled. Retry or choose a building."
                LocationStatus.Stale -> "This location snapshot has expired. Retry or choose a building."
            }
            if (message != null) {
                Text(
                    message, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            state.start?.let { start ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val name = when (start) {
                        is StartingPoint.Building -> CampusBuildings.find(start.buildingId)?.name.orEmpty()
                        is StartingPoint.Device -> "My location (snapshot)"
                    }
                    TextButton(onClick = session::clear) { Text("Clear starting point: $name") }
                }
            }
        }
    }
}
