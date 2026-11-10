package com.example.cipod_final.indoor

import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.sierra_hall_first_floor_draft

object FloorPlanCatalog {
    // Fictional test canvases only. These do not assert any Sierra Hall floor count or layout.
    val plans: List<FloorPlan> = listOf(
        FloorPlan("sierra-demo-a", "sierra-hall", "Demo layout A", "demo-1", 1.4f,
            FloorPlanContent.Demonstration("A")),
        FloorPlan("sierra-demo-b", "sierra-hall", "Demo layout B", "demo-1", 0.85f,
            FloorPlanContent.Demonstration("B")),
        FloorPlan("sierra-first-floor-reference-draft", "sierra-hall",
            "First Floor • Reference draft", "reference-draft-2", 1600f / 2372f,
            FloorPlanContent.ReferenceDraftImage(Res.drawable.sierra_hall_first_floor_draft))
    )

    fun forBuilding(id: String): List<FloorPlan> = plans.filter { it.buildingId == id }
}
