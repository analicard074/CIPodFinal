package com.example.cipod_final.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include

@Composable
fun CampusMapScreen(modifier: Modifier = Modifier) {
    val mapState = rememberMapState(
        baseStyle = CampusMapConfig.baseStyle,
        initialCameraPosition = CampusMapConfig.initialCameraPosition
    )

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Explore CI", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Drag to explore campus. Pinch or use + / − to zoom.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        MaplibreMap(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = mapState
        ) {
            // Full includes zoom buttons, the MapLibre logo, and the source attribution.
            // Keep these visible above HomePage's bottom navigation.
            include(MapOverlay.Full)
        }
    }
}
