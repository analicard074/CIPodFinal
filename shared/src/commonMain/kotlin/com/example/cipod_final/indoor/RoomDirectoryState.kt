package com.example.cipod_final.indoor

data class RoomDirectoryState(
    val buildingId: String,
    val query: String = "",
    val selectedRoomId: String? = null
) {
    val results: List<DirectoryRoom> get() = RoomDirectory.search(buildingId, query)
    val selectedRoom: DirectoryRoom? get() = RoomDirectory.find(buildingId, selectedRoomId)
    fun select(roomId: String): RoomDirectoryState =
        if (RoomDirectory.find(buildingId, roomId) != null) copy(selectedRoomId = roomId) else this
    fun clearSelection(): RoomDirectoryState = copy(selectedRoomId = null)
}
