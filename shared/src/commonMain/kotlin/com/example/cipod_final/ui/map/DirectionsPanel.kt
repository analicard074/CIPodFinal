package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cipod_final.routing.PedestrianRouteResult

@Composable
internal fun DirectionsPanel(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    startLabel: String,
    destinationName: String,
    roomLabel: String?,
    routeResult: PedestrianRouteResult?,
    accessibilityEnabled: Boolean,
    supportsRouteActions: Boolean,
    onFindRoute: () -> Unit,
    onChooseRoom: (() -> Unit)?,
    onOpenIndoorPlan: (() -> Unit)?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Directions", style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (val result = routeResult) {
                            is PedestrianRouteResult.Available ->
                                "$startLabel → $destinationName • ${result.distanceMeters.toInt()} m"
                            is PedestrianRouteResult.Unavailable -> "Route unavailable"
                            else -> "$startLabel → $destinationName"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }

            if (expanded) {
                Text("From: $startLabel", style = MaterialTheme.typography.bodySmall)
                Text("To: $destinationName", style = MaterialTheme.typography.bodySmall)
                if (roomLabel != null) {
                    Text(roomLabel, style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    routeMessage(routeResult, accessibilityEnabled, supportsRouteActions),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (supportsRouteActions) {
                        TextButton(onClick = onFindRoute) { Text("Find walking route") }
                        onChooseRoom?.let { TextButton(onClick = it) { Text("Choose room") } }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    onOpenIndoorPlan?.let { TextButton(onClick = it) { Text("Open indoor plan") } }
                    TextButton(onClick = onClear) { Text("Clear route & destination") }
                }
            }
        }
    }
}

private fun routeMessage(
    result: PedestrianRouteResult?,
    accessibilityEnabled: Boolean,
    supportsRouteActions: Boolean
): String = when (result) {
    is PedestrianRouteResult.Available -> if (result.accessibilityVerified) {
        "Step-free route verified • ${result.distanceMeters.toInt()} m"
    } else {
        "Walking route • ${result.distanceMeters.toInt()} m"
    }
    is PedestrianRouteResult.Unavailable -> when (result.reason) {
        PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED ->
            "Route unavailable. Accessibility has not been verified for the walkway network and entrance."
        PedestrianRouteResult.Reason.LOCATION_NOT_USABLE ->
            "Route unavailable: use a fresh, precise GPS reading."
        PedestrianRouteResult.Reason.OUTSIDE_COVERAGE -> "Route unavailable from this location."
        PedestrianRouteResult.Reason.NO_NEARBY_WALKWAY ->
            "Route unavailable: no supported walkway is close enough."
        PedestrianRouteResult.Reason.ENTRANCE_NOT_VERIFIED ->
            "Route unavailable: a verified entrance connection is missing."
        PedestrianRouteResult.Reason.NO_CONNECTED_ROUTE ->
            "Route unavailable: no connected supported walkway route was found."
        PedestrianRouteResult.Reason.NETWORK_NOT_VERIFIED ->
            "Route unavailable: verified walkway data and an entrance connection are not available."
    }
    null -> when {
        accessibilityEnabled ->
            "Accessibility has not been verified for the walkway network and entrance."
        supportsRouteActions -> "Choose Find walking route to check this route."
        else -> "Outdoor walking route data is not available for this building."
    }
}
