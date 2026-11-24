package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuilding

@Composable
internal fun BuildingDetailsCard(
    building: CampusBuilding,
    destination: CampusBuilding?,
    onSelectDestination: () -> Unit,
    onSelectStartingPoint: () -> Unit,
    onViewFloorPlans: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    var detailsExpanded by remember(building.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row {
                Text(
                    building.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onClose) { Text("Close") }
            }
            TextButton(onClick = { detailsExpanded = !detailsExpanded }) {
                Text(if (detailsExpanded) "Hide building details" else "Show building details")
            }
            if (detailsExpanded) {
                Text(building.description, style = MaterialTheme.typography.bodyMedium)
                Text(building.locationNote, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onSelectDestination,
                enabled = building.coordinates != null && destination?.id != building.id,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        destination?.id == building.id -> "Current destination"
                        destination != null -> "Replace destination"
                        else -> "Set destination"
                    }
                )
            }
            if (onViewFloorPlans != null) {
                TextButton(onClick = onViewFloorPlans) { Text("View Floor Plans") }
            }
            TextButton(
                onClick = onSelectStartingPoint,
                enabled = building.coordinates != null
            ) { Text("Set as starting point") }
        }
    }
}
