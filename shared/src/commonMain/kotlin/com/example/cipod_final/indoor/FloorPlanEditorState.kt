package com.example.cipod_final.indoor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Session-only drafts. Normal builds cannot mutate or export even through these action methods. */
class FloorPlanEditorState(val developerEnabled: Boolean = false) {
    private val drafts = mutableStateMapOf<String, List<RoomMarker>>()
    private val originals = mutableMapOf<String, List<RoomMarker>>()
    var editing by mutableStateOf(false)
        private set

    fun enableEditing(value: Boolean) { editing = developerEnabled && value }
    fun loaded(plan: FloorPlan): Boolean = plan.id in drafts
    fun markers(plan: FloorPlan): List<RoomMarker> = drafts[plan.id].orEmpty()
    fun dirty(plan: FloorPlan): Boolean = loaded(plan) && drafts[plan.id] != originals[plan.id]

    fun load(plan: FloorPlan, markers: List<RoomMarker>) {
        if (loaded(plan)) return
        require(markers.map { it.id }.distinct().size == markers.size)
        originals[plan.id] = markers.toList()
        drafts[plan.id] = markers.toList()
    }

    fun add(plan: FloorPlan, type: RoomMarkerType, point: PlanPoint): RoomMarker? {
        if (!developerEnabled || !editing || !loaded(plan)) return null
        var number = 1
        while (markers(plan).any { it.id == "marker-$number" }) number++
        val name = if (plan.isDemonstration) "Demo ${type.label.lowercase()} $number" else "New ${type.label.lowercase()}"
        val marker = RoomMarker("marker-$number", name, type, point)
        drafts[plan.id] = markers(plan) + marker
        return marker
    }

    fun move(plan: FloorPlan, id: String, point: PlanPoint): Boolean = replace(plan, id) { it.copy(position = point) }

    fun update(plan: FloorPlan, id: String, name: String, type: RoomMarkerType): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 80) return false
        return replace(plan, id) { it.copy(name = trimmed, type = type) }
    }

    private fun replace(plan: FloorPlan, id: String, change: (RoomMarker) -> RoomMarker): Boolean {
        if (!developerEnabled || !editing || markers(plan).none { it.id == id }) return false
        drafts[plan.id] = markers(plan).map { if (it.id == id) change(it) else it }
        return true
    }

    fun delete(plan: FloorPlan, id: String): Boolean {
        if (!developerEnabled || !editing || markers(plan).none { it.id == id }) return false
        drafts[plan.id] = markers(plan).filterNot { it.id == id }
        return true
    }

    fun export(plan: FloorPlan): String? =
        if (developerEnabled && loaded(plan)) FloorPlanJson.encode(plan, markers(plan)) else null
}
