package com.example.cipod_final.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.util.MaplibreComposable

/**
 * MapLibre normally saves its camera center through rememberSaveable. A camera centered on a
 * student is also sensitive location data, so disable saved-state registration for this map.
 * Building IDs and search outside this scope keep their existing restoration behavior.
 */
@Composable
internal fun rememberEphemeralCampusMapState(
    content: @Composable @MaplibreComposable () -> Unit
): MapState {
    lateinit var state: MapState
    CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        state = rememberMapState(
            baseStyle = CampusMapConfig.baseStyle,
            initialCameraPosition = CampusMapConfig.initialCameraPosition,
            content = content
        )
    }
    return state
}
