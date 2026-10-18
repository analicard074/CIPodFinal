package com.example.cipod_final.buildings

/** A verified building-level point, not a surveyed entrance or routing endpoint. */
data class BuildingCoordinates(val latitude: Double, val longitude: Double)

data class CampusBuilding(
    val id: String,
    val name: String,
    val description: String,
    val coordinates: BuildingCoordinates?,
    val universitySource: String,
    val geographicSource: String? = null,
    val locationNote: String = "Building location; entrances may differ."
)
