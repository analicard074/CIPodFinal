package com.example.cipod_final.indoor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** One reviewed plan revision per document. Verification comes from the catalog, never an import. */
object FloorPlanJson {
    private val json = Json { prettyPrint = true }

    fun encode(plan: FloorPlan, markers: List<RoomMarker>): String {
        require(markers.map { it.id }.distinct().size == markers.size)
        val document = buildJsonObject {
            put("schemaVersion", 1)
            put("buildingId", plan.buildingId)
            put("floorPlanId", plan.id)
            put("planRevision", plan.revision)
            put("demonstration", plan.isDemonstration)
            put("markers", JsonArray(markers.map { marker -> buildJsonObject {
                put("id", marker.id)
                put("name", marker.name)
                put("type", marker.type.code)
                put("x", marker.position.x)
                put("y", marker.position.y)
            } }))
        }
        return json.encodeToString(JsonObject.serializer(), document)
    }

    fun decode(plan: FloorPlan, text: String): List<RoomMarker> {
        val document = json.parseToJsonElement(text).jsonObject
        require(document.getValue("schemaVersion").jsonPrimitive.int == 1)
        require(document.getValue("buildingId").jsonPrimitive.content == plan.buildingId)
        require(document.getValue("floorPlanId").jsonPrimitive.content == plan.id)
        require(document.getValue("planRevision").jsonPrimitive.content == plan.revision)
        require(document.getValue("demonstration").jsonPrimitive.boolean == plan.isDemonstration)
        val markers = document.getValue("markers").jsonArray.map { value ->
            val marker = value.jsonObject
            RoomMarker(
                marker.getValue("id").jsonPrimitive.content,
                marker.getValue("name").jsonPrimitive.content,
                RoomMarkerType.entries.first { it.code == marker.getValue("type").jsonPrimitive.content },
                PlanPoint(marker.getValue("x").jsonPrimitive.float, marker.getValue("y").jsonPrimitive.float)
            )
        }
        require(markers.map { it.id }.distinct().size == markers.size) { "Duplicate marker IDs" }
        return markers
    }
}
