package com.example.cipod_final.indoor

import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.sierra_hall_first_floor
import cipodfinal.shared.generated.resources.broome_library_floor_1
import cipodfinal.shared.generated.resources.broome_library_floor_2

object FloorPlanCatalog {
    // Sierra Hall first-floor reference layout
    //Draft based on campus evacuation plan, room locations are not yet verified.
    val plans: List<FloorPlan> = listOf(
        FloorPlan("sierra-first-floor-reference-draft", "sierra-hall",
            "First Floor • Reference draft", "reference-draft-2", 1600f / 2372f,
            FloorPlanContent.ReferenceDraftImage(Res.drawable.sierra_hall_first_floor)),
        FloorPlan("broome-library-first-floor-draft", "broome-library",
            "First Floor • Reference draft", "reference-draft-1", 2414f / 2149f,
            FloorPlanContent.ReferenceDraftImage(Res.drawable.broome_library_floor_1)),
        FloorPlan("broome-library-second-floor-draft", "broome-library",
            "Second Floor • Reference draft", "reference-draft-1", 1224f / 1105f,
            FloorPlanContent.ReferenceDraftImage(Res.drawable.broome_library_floor_2)),
    )

    fun forBuilding(id: String): List<FloorPlan> = plans.filter { it.buildingId == id }
}
