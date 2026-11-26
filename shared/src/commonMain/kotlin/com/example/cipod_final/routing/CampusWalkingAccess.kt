package com.example.cipod_final.routing

import com.example.cipod_final.buildings.BuildingCoordinates

/**
 * Routing points are separate from building labels. These are OSM-mapped pedestrian access
 * points, not surveyed accessible entrances. Evidence and review date are in docs/maps/README.md.
 */
internal object CampusWalkingAccess {
    fun departureFor(buildingId: String, destinationId: String): BuildingCoordinates? {
        if (buildingId == "sierra-hall" && destinationId == "broome-library") {
            return BuildingCoordinates(34.1622379, -119.0443425)
        }
        return forBuilding(buildingId)
    }

    fun forBuilding(buildingId: String): BuildingCoordinates? = when (buildingId) {
        // Main entrance, on footways 955221403 / 1361026267.
        "sierra-hall" -> BuildingCoordinates(34.1621084, -119.0444844)
        // West end of covered footway 1170524631, connected to approach 817294745.
        "broome-library" -> BuildingCoordinates(34.1626619, -119.0410829)
        // Building-side end of footway 955221409; entrance not tagged in OSM.
        "gateway-hall" -> BuildingCoordinates(34.1649277, -119.0448354)
        else -> null
    }
}
