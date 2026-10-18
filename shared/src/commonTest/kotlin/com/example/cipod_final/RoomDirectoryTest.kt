package com.example.cipod_final

import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.placeholder_map
import com.example.cipod_final.indoor.*
import kotlin.test.*

class RoomDirectoryTest {
    private val room = RoomDirectory.find("sierra-hall", "sierra-hall-1111")!!
    // Test fixture only: does not add an approved plan to the production catalog.
    private val plan = FloorPlan("test-approved", "sierra-hall", "Test fixture", "r1", 1f,
        FloorPlanContent.ApprovedImage(Res.drawable.placeholder_map), "https://example.test/plan", "2026-10-18")
    private val location = VerifiedRoomLocation(room.id, room.buildingId, plan.id, plan.revision,
        PlanPoint(.2f, .3f), "https://example.test/review", "2026-10-18")

    @Test fun exactPublishedRoomSetAndTypes() {
        val expected = mapOf(
            "1111" to "Classroom", "1121" to "Classroom", "1131" to "Computer Lab",
            "1141" to "Computer Lab", "1212" to "Computer Lab", "1222" to "Computer Lab",
            "1232" to "Computer Lab", "1242" to "Computer Lab", "1314" to "Lab",
            "1324" to "Lab", "1334" to "Lab", "1344" to "Lab", "1411" to "Classroom",
            "1422" to "Classroom", "1432" to "Computer Lab")
        val rooms = RoomDirectory.search("sierra-hall")
        assertEquals(15, rooms.size)
        assertEquals(expected, rooms.associate { it.number to it.publishedType })
        assertEquals(15, rooms.map { it.id }.distinct().size)
        assertTrue(rooms.all { it.sourceUrl == RoomDirectory.sourceUrl && it.reviewedOn.isNotBlank() })
    }
    @Test fun searchTrimsAndMatchesPartialNumbers() {
        assertEquals(listOf("1111", "1121", "1131", "1141", "1411"),
            RoomDirectory.search("sierra-hall", " 11 ").map { it.number })
    }
    @Test fun searchTypeIgnoresCase() {
        assertEquals(7, RoomDirectory.search("sierra-hall", " COMPUTER lab ").size)
        assertEquals(4, RoomDirectory.search("sierra-hall", "classroom").size)
    }
    @Test fun emptyAndUnknownSearch() {
        assertEquals(15, RoomDirectory.search("sierra-hall", "  ").size)
        assertTrue(RoomDirectory.search("sierra-hall", "9999").isEmpty())
    }
    @Test fun buildingIsolation() {
        assertTrue(RoomDirectory.search("broome-library").isEmpty())
        assertTrue(RoomDirectory.search("gateway-hall").isEmpty())
        val state = RoomDirectoryState("broome-library")
        assertEquals(state, state.select(room.id))
        assertNull(RoomDirectory.find("broome-library", room.id))
    }
    @Test fun selectReplaceAndClear() {
        val state = RoomDirectoryState("sierra-hall").select(room.id)
        assertEquals("1111", state.selectedRoom?.number)
        assertEquals("Classroom", state.selectedRoom?.publishedType)
        val replaced = state.select("sierra-hall-1432")
        assertEquals("1432", replaced.selectedRoom?.number)
        assertNull(replaced.clearSelection().selectedRoom)
        assertEquals(state, state.select("missing"))
    }
    @Test fun searchingPreservesSelectedRoom() {
        val state = RoomDirectoryState("sierra-hall").select(room.id).copy(query = "computer lab")
        assertEquals(room, state.selectedRoom)
        assertEquals(7, state.results.size)
    }
    @Test fun allProductionRoomsHavePendingPositions() {
        RoomDirectory.rooms.forEach { assertNull(RoomLocationRegistry.current.find(it, FloorPlanCatalog.plans)) }
        assertEquals("Room location pending verification", RoomLocationRegistry.pendingMessage)
    }
    @Test fun reviewedPositionResolvesOnlyForMatchingApprovedPlan() {
        val registry = RoomLocationRegistry(listOf(location))
        assertEquals(location, registry.find(room, listOf(plan)))
        assertNull(registry.find(room, emptyList()))
        assertNull(registry.find(room, listOf(plan.copy(revision = "r2"))))
        assertNull(registry.find(room, listOf(plan.copy(id = "another-plan"))))
        assertNull(registry.find(room, listOf(plan.copy(buildingId = "broome-library"))))
    }
    @Test fun demonstrationCannotVerifyRoomPosition() {
        val demo = FloorPlanCatalog.plans.first()
        val registry = RoomLocationRegistry(listOf(location.copy(floorPlanId = demo.id, planRevision = demo.revision)))
        assertNull(registry.find(room, listOf(demo)))
    }
    @Test fun missingEvidenceOrWrongRoomCannotResolve() {
        listOf(location.copy(sourceUrl = ""), location.copy(reviewedOn = ""),
            location.copy(buildingId = "gateway-hall"), location.copy(roomId = "another-room")).forEach {
            assertNull(RoomLocationRegistry(listOf(it)).find(room, listOf(plan)))
        }
    }
}
