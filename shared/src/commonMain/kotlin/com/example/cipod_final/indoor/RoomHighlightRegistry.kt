package com.example.cipod_final.indoor

object RoomHighlightRegistry {

    val highlights = listOf(
        RoomHighlight(
            roomId = "broome-library-2330",
            floorPlanId = "broome-library-second-floor-draft",
            planRevision = "reference-draft-1",
            // Interior edge of Classroom 2330 measured on the current 1224 x 1105 PNG.
            left = 0.133987f, // x = 164 / 1224
            top = 0.418100f, // y = 462 / 1105
            right = 0.235294f, // x = 288 / 1224
            bottom = 0.524887f // y = 580 / 1105
        )
    )

    fun find(
        roomId: String,
        floorPlanId: String,
        planRevision: String
    ): RoomHighlight? =
        highlights.firstOrNull {
            it.roomId == roomId &&
                    it.floorPlanId == floorPlanId &&
                    it.planRevision == planRevision
        }
}
