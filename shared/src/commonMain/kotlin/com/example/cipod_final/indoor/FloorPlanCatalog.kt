package com.example.cipod_final.indoor

object FloorPlanCatalog {
    // Fictional test canvases only. These do not assert any Sierra Hall floor count or layout.
    val plans: List<FloorPlan> = listOf(
        FloorPlan("sierra-demo-a", "sierra-hall", "Demo layout A", "demo-1", 1.4f,
            FloorPlanContent.Demonstration("A")),
        FloorPlan("sierra-demo-b", "sierra-hall", "Demo layout B", "demo-1", 0.85f,
            FloorPlanContent.Demonstration("B"))
    )

    fun forBuilding(id: String): List<FloorPlan> = plans.filter { it.buildingId == id }
}
