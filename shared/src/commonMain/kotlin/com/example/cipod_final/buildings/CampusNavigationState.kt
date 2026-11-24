package com.example.cipod_final.buildings

/** Browsing details never changes the destination until the student explicitly selects it. */
data class CampusNavigationState(
    val query: String = "",
    val selectedBuildingId: String? = null,
    val destinationId: String? = null,
    val destinationRoomId: String? = null,
    val destinationFloorPlanId: String? = null
) {
    val results: List<CampusBuilding> get() = CampusBuildings.search(query)
    val selectedBuilding: CampusBuilding? get() = CampusBuildings.find(selectedBuildingId)
    val destination: CampusBuilding? get() = CampusBuildings.find(destinationId)

    fun preview(id: String): CampusNavigationState =
        if (CampusBuildings.find(id) != null) copy(selectedBuildingId = id) else this

    fun selectDestination(id: String): CampusNavigationState =
        if (CampusBuildings.find(id)?.coordinates != null) {
            val keepRoom = destinationId == id
            copy(
                destinationId = id,
                selectedBuildingId = id,
                destinationRoomId = if (keepRoom) destinationRoomId else null,
                destinationFloorPlanId = if (keepRoom) destinationFloorPlanId else null
            )
        } else this

    fun selectRoom(roomId: String?, floorPlanId: String?): CampusNavigationState =
        copy(destinationRoomId = roomId, destinationFloorPlanId = if (roomId == null) null else floorPlanId)

    fun clearDestination(): CampusNavigationState =
        copy(destinationId = null, destinationRoomId = null, destinationFloorPlanId = null)
}
