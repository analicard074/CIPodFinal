package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuilding

@Composable
internal fun BuildingDetailsCard(
    building: CampusBuilding,
    navigationActive: Boolean,
    onSelectStartingPoint: () -> Unit,
    onDirections: () -> Unit,
    onOpenRoomDirectory: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(16.dp)
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        building.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FilledTonalButton(
                        onClick = onOpenRoomDirectory,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text("Room Directory", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text("Close", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
                Text(
                    building.description,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    building.locationNote,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onSelectStartingPoint,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = building.coordinates != null && !navigationActive,
                ) { Text("Start here") }
                Button(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = building.coordinates != null && !navigationActive,
                ) { Text("Directions") }
            }
        }
    }
}
