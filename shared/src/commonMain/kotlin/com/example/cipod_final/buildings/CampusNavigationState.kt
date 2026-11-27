package com.example.cipod_final.buildings

import com.example.cipod_final.indoor.BroomeIndoorRoutes
import com.example.cipod_final.indoor.DirectoryRoom
import com.example.cipod_final.indoor.RoomDirectory

/** Browsing details never changes the destination until the student explicitly selects it. */
data class CampusNavigationState(
    val query: String = "",
    val selectedBuildingId: String? = null,
    val destinationId: String? = null,
    val destinationRoomId: String? = null,
    val startRoomId: String? = null,
    val destinationContext: String? = null,
) {
    val results: List<CampusBuilding> get() = CampusBuildings.search(query)
    val selectedBuilding: CampusBuilding? get() = CampusBuildings.find(selectedBuildingId)
    val destination: CampusBuilding? get() = CampusBuildings.find(destinationId)
    val destinationRoom: DirectoryRoom? get() = destinationId?.let { RoomDirectory.find(it, destinationRoomId) }
    val startRoom: DirectoryRoom? get() = RoomDirectory.find("broome-library", startRoomId)

    fun selectRoomStart(room: DirectoryRoom): CampusNavigationState {
        if (!canRouteTo(room)) return this
        val sameBuilding = destinationId == room.buildingId
        return copy(
            startRoomId = room.id,
            selectedBuildingId = room.buildingId,
            destinationId = if (sameBuilding) destinationId else null,
            destinationRoomId = if (sameBuilding) destinationRoomId else null,
            destinationContext = if (sameBuilding) destinationContext else null,
        )
    }

    fun clearRoomStart(): CampusNavigationState = copy(startRoomId = null)

    fun selectRoomDestination(room: DirectoryRoom): CampusNavigationState {
        if (!canRouteTo(room) || CampusBuildings.find(room.buildingId)?.coordinates == null) return this
        return copy(
            destinationId = room.buildingId,
            selectedBuildingId = room.buildingId,
            destinationRoomId = room.id,
            startRoomId = if (startRoom?.buildingId == room.buildingId) startRoomId else null,
            destinationContext = null,
        )
    }

    private fun canRouteTo(room: DirectoryRoom): Boolean =
        RoomDirectory.find(room.buildingId, room.id) == room &&
            BroomeIndoorRoutes.current()?.supports(room) == true

    fun preview(id: String): CampusNavigationState =
        if (CampusBuildings.find(id) != null) copy(selectedBuildingId = id) else this

    fun selectDestination(id: String): CampusNavigationState =
        if (CampusBuildings.find(id)?.coordinates != null) {
            copy(
                destinationId = id,
                selectedBuildingId = id,
                destinationRoomId = null,
                startRoomId = null,
                destinationContext = null,
            )
        } else this

    fun selectResourceDestination(
        id: String,
        resourceName: String,
        indoorRoomId: String? = null,
    ): CampusNavigationState {
        val selected = selectDestination(id)
        if (selected.destinationId != id) return this

        // Resolve through the shared room directory and existing route-support checks. An absent
        // or unsupported room leaves a valid building-only destination with no stale room state.
        val withRoom = indoorRoomId
            ?.let { RoomDirectory.find(id, it) }
            ?.let(selected::selectRoomDestination)
            ?: selected
        return withRoom.copy(destinationContext = resourceName.trim().takeIf { it.isNotEmpty() })
    }

    fun clearDestination(): CampusNavigationState =
        copy(destinationId = null, destinationRoomId = null, destinationContext = null)
}
