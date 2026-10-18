package com.example.cipod_final.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cipod_final.buildings.CampusBuilding

@Composable
internal fun BuildingSearchBar(
    query: String,
    results: List<CampusBuilding>,
    onQueryChange: (String) -> Unit,
    onBuildingClick: (CampusBuilding) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Explore CI", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search campus buildings") },
                placeholder = { Text("Try Sierra Hall") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    if (query.isNotEmpty()) TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
                }
            )
            if (results.isEmpty()) {
                Text("No buildings found. Try another name.", Modifier.padding(12.dp))
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 144.dp)) {
                    items(results, key = { it.id }) { building ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onBuildingClick(building) }
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text(building.name, style = MaterialTheme.typography.titleSmall)
                            if (building.coordinates == null) {
                                Text("Location awaiting verification", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
