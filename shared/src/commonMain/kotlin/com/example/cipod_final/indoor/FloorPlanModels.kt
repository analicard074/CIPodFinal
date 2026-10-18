package com.example.cipod_final.indoor

import org.jetbrains.compose.resources.DrawableResource

/** Demo IDs and labels never describe a real floor number or real room. */
sealed interface FloorPlanContent {
    data class Demonstration(val variant: String) : FloorPlanContent
    data class ApprovedImage(val resource: DrawableResource) : FloorPlanContent
}

data class FloorPlan(
    val id: String,
    val buildingId: String,
    val label: String,
    val revision: String,
    val aspectRatio: Float,
    val content: FloorPlanContent,
    val sourceUrl: String? = null,
    val reviewedOn: String? = null
) {
    val isDemonstration: Boolean get() = content is FloorPlanContent.Demonstration
    init {
        require(id.isNotBlank() && buildingId.isNotBlank() && revision.isNotBlank())
        require(aspectRatio.isFinite() && aspectRatio > 0)
        require(isDemonstration || (!sourceUrl.isNullOrBlank() && !reviewedOn.isNullOrBlank())) {
            "Approved plans require a reviewed source; editing does not verify a plan."
        }
    }
}

data class PlanPoint(val x: Float, val y: Float) {
    init { require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f) }
}
