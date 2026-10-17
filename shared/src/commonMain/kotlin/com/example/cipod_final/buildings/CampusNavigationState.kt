package com.example.cipod_final.buildings

/** Browsing details never changes the destination until the student explicitly selects it. */
data class CampusNavigationState(
    val query: String = "",
    val selectedBuildingId: String? = null,
    val destinationId: String? = null
) {
    val results: List<CampusBuilding> get() = CampusBuildings.search(query)
    val selectedBuilding: CampusBuilding? get() = CampusBuildings.find(selectedBuildingId)
    val destination: CampusBuilding? get() = CampusBuildings.find(destinationId)

    fun preview(id: String): CampusNavigationState =
        if (CampusBuildings.find(id) != null) copy(selectedBuildingId = id) else this

    fun selectDestination(id: String): CampusNavigationState =
        if (CampusBuildings.find(id)?.coordinates != null) {
            copy(destinationId = id, selectedBuildingId = id)
        } else this

    fun clearDestination(): CampusNavigationState = copy(destinationId = null)
}
