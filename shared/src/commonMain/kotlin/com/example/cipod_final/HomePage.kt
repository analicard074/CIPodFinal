package com.example.cipod_final

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.cipod_final.buildings.CampusNavigationState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cipod_final.ui.map.CampusMapScreen

@Composable
fun HomePage(modifier: Modifier = Modifier)
{
    var selected by remember { mutableStateOf("Directions") }

    var navigation by rememberSaveable(stateSaver = listSaver<CampusNavigationState, String>(
        save = { listOf(it.query, it.selectedBuildingId.orEmpty(), it.destinationId.orEmpty()) },
        restore = { CampusNavigationState(it[0], it[1].ifEmpty { null }, it[2].ifEmpty { null }) }
    )) { mutableStateOf(CampusNavigationState()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {
                TextButton(
                    onClick = { selected = "Directions" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Directions")
                }
                TextButton(
                    onClick = { selected = "MyCI" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("MyCI")
                }
                TextButton(
                    onClick = { selected = "Settings" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings")
                }
            }
        },
    ) { contentPadding ->
        if (selected == "Directions") {
            CampusMapScreen(
                navigation = navigation,
                onNavigationChange = { navigation = it },
                modifier = Modifier.fillMaxSize().padding(contentPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp)
            ) {
                Text(selected)
                Text("$selected content will appear here.")
            }
        }
    }
}
