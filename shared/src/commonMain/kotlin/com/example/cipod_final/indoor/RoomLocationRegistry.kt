package com.example.cipod_final.indoor

/** Separate, reviewed association to an exact approved image revision; never editor drafts. */
data class VerifiedRoomLocation(
    val roomId: String,
    val buildingId: String,
    val floorPlanId: String,
    val planRevision: String,
    val position: PlanPoint,
    val sourceUrl: String,
    val reviewedOn: String
)

class RoomLocationRegistry(
    private val locations: List<VerifiedRoomLocation> = emptyList()
) {
    fun find(room: DirectoryRoom, plans: List<FloorPlan>): VerifiedRoomLocation? =
        locations.firstOrNull { location ->
            location.roomId == room.id && location.buildingId == room.buildingId &&
                location.sourceUrl.isNotBlank() && location.reviewedOn.isNotBlank() &&
                plans.any { plan ->
                    !plan.isDemonstration && plan.buildingId == room.buildingId &&
                        plan.id == location.floorPlanId && plan.revision == location.planRevision
                }
        }

    companion object {
        // No Sierra Hall indoor positions have been verified.
        val current = RoomLocationRegistry()
        const val pendingMessage = "Room location pending verification"
    }
}
