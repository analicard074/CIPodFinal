package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuilding

@Composable
internal fun BuildingDetailsCard(
    building: CampusBuilding,
    destination: CampusBuilding?,
    onSelectDestination: () -> Unit,
    onSelectStartingPoint: () -> Unit,
    onClose: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row {
                Text(building.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Close") }
            }
            Text(building.description, style = MaterialTheme.typography.bodyMedium)
            Text(building.locationNote, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onSelectDestination,
                enabled = building.coordinates != null && destination?.id != building.id,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(when {
                    destination?.id == building.id -> "Current destination"
                    destination != null -> "Replace destination"
                    else -> "Set destination"
                })
            }
            TextButton(
                onClick = onSelectStartingPoint,
                enabled = building.coordinates != null
            ) { Text("Set as starting point") }
        }
    }
}
