package com.example.cipod_final.indoor

import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.sierra_hall_first_floor

object FloorPlanCatalog {
    // Sierra Hall first-floor reference layout
    //Draft based on campus evacuation plan, room locations are not yet verified.
    val plans: List<FloorPlan> = listOf(
        FloorPlan("sierra-first-floor-reference-draft", "sierra-hall",
            "First Floor • Reference draft", "reference-draft-2", 1600f / 2372f,
            FloorPlanContent.ReferenceDraftImage(Res.drawable.sierra_hall_first_floor))
    )

    fun forBuilding(id: String): List<FloorPlan> = plans.filter { it.buildingId == id }
}
