package com.example.cipod_final.ui.map

import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

internal object CampusMapConfig {
    // Campus overview only; this is not an entrance or routing destination.
    val initialCameraPosition = CameraPosition(
        center = Position(latitude = 34.1622, longitude = -119.0434),
        zoom = 15.5
    )

    val baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/positron")
}
