package com.example.cipod_final.ui.map

import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.CameraConstraints
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

internal object CampusMapConfig {
    val campusBounds = BoundingBox(
        west = -119.0512,
        south = 34.1558,
        east = -119.0355,
        north = 34.1705
    )
    val cameraConstraints = CameraConstraints {
        boundingBox = campusBounds
        minZoom = 15.0
    }

    // A location is outside campus if it passes any of the four boundaries
    fun isOutsideCampus(latitude: Double, longitude: Double): Boolean {
        return latitude < campusBounds.south ||
                latitude > campusBounds.north ||
                longitude < campusBounds.west ||
                longitude > campusBounds.east
    }

    // Covers Gateway Hall, Sierra Hall, and Broome Library
    val buildingBounds = BoundingBox(
        west = -119.045057,
        south = 34.162197,
        east = -119.041100,
        north = 34.164838
    )

    // Campus overview only; this is not an entrance or routing destination.
    val initialCameraPosition = CameraPosition(
        center = Position(latitude = 34.1622, longitude = -119.0434),
        zoom = 15.5
    )

    val baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/positron")
}
