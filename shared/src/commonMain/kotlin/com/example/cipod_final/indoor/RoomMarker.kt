package com.example.cipod_final.indoor

enum class RoomMarkerType(val label: String, val code: String, val symbol: String) {
    Classroom("Classroom", "classroom", "C"), Restroom("Restroom", "restroom", "R"),
    Entrance("Entrance", "entrance", "E"), Stair("Stair", "stair", "S"),
    Elevator("Elevator", "elevator", "L")
}

data class RoomMarker(val id: String, val name: String, val type: RoomMarkerType, val position: PlanPoint) {
    init {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,80}")))
        require(name.isNotBlank() && name.length <= 80)
    }
}

object IndoorEditorPolicy {
    fun allowed(debugBuild: Boolean, explicitlyRequested: Boolean): Boolean = debugBuild && explicitlyRequested
}
