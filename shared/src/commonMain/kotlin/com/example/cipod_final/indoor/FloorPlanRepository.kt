package com.example.cipod_final.indoor

import cipodfinal.shared.generated.resources.Res

object FloorPlanRepository {
    suspend fun load(plan: FloorPlan): List<RoomMarker> = FloorPlanJson.decode(
        plan, Res.readBytes("files/indoor/${plan.id}.json").decodeToString()
    )
}
