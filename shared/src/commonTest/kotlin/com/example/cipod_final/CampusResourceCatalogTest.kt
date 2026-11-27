package com.example.cipod_final

import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.data.CampusResourceCatalog
import com.example.cipod_final.data.CampusResourceCategory
import com.example.cipod_final.indoor.BroomeIndoorRoutes
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.IndoorConnection
import com.example.cipod_final.indoor.RoomDirectory
import com.example.cipod_final.ui.map.WalkingRouteState
import com.example.cipod_final.routing.CampusWalkingAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampusResourceCatalogTest {
    @Test
    fun searchMatchesNamesCategoriesAndDescriptionsWithoutCaseSensitivity() {
        assertEquals("dolphin-pantry", CampusResourceCatalog.search("DOLPHIN").single().id)
        assertTrue(CampusResourceCatalog.search("academic support").isNotEmpty())
        assertTrue(CampusResourceCatalog.search("peer tutoring").any { it.id == "learning-resource-center" })
    }

    @Test
    fun categoryFilterAndClearSearchReturnExpectedResources() {
        val health = CampusResourceCatalog.search("", CampusResourceCategory.HEALTH_WELLNESS)
        assertTrue(health.isNotEmpty())
        assertTrue(health.all { it.category == CampusResourceCategory.HEALTH_WELLNESS })
        assertEquals(CampusResourceCatalog.all, CampusResourceCatalog.search(" "))
    }

    @Test
    fun catalogHasUniqueIdsOfficialLinksAndAllRequestedCategories() {
        assertEquals(CampusResourceCatalog.all.size, CampusResourceCatalog.all.map { it.id }.distinct().size)
        assertTrue(CampusResourceCatalog.all.all { it.officialUrl.startsWith("https://www.csuci.edu/") })
        assertEquals(
            CampusResourceCategory.entries.toSet(),
            CampusResourceCatalog.all.map { it.category }.toSet(),
        )
    }

    @Test
    fun onlyResourcesWithReviewedMapEndpointsCanRequestDirections() {
        val lrc = assertNotNull(CampusResourceCatalog.find("learning-resource-center"))
        val buildingId = assertNotNull(lrc.directionsBuildingId)
        assertEquals("broome-library", buildingId)
        assertNotNull(CampusBuildings.find(buildingId))
        assertNotNull(CampusWalkingAccess.forBuilding(buildingId))

        val financialAid = assertNotNull(CampusResourceCatalog.find("financial-aid-scholarships"))
        assertNull(financialAid.location)
        assertFalse(financialAid.locationNote.isNullOrBlank())
        assertTrue(CampusResourceCatalog.all.filter { it.id != lrc.id }.all { it.directionsBuildingId == null })
    }

    @Test
    fun financialAidResourceUsesTheCurrentOfficialHomepage() {
        val financialAid = assertNotNull(CampusResourceCatalog.find("financial-aid-scholarships"))
        assertEquals("https://www.csuci.edu/cost-aid/financial-aid/", financialAid.officialUrl)
    }

    @Test
    fun lrcDirectionsReuseTheExistingRoom2730SecondFloorRouteAndAllowBothConnectors() {
        val lrc = assertNotNull(CampusResourceCatalog.find("learning-resource-center"))
        assertEquals("broome-library-2730", lrc.indoorDestinationRoomId)
        val room = assertNotNull(RoomDirectory.find("broome-library", lrc.indoorDestinationRoomId))
        assertEquals("2730", room.number)
        assertEquals("broome-library-second-floor-draft", room.floorPlanId)
        assertTrue(FloorPlanCatalog.forBuilding("broome-library").any { it.id == room.floorPlanId })

        val state = CampusNavigationState().selectResourceDestination(
            lrc.directionsBuildingId!!, lrc.name, lrc.indoorDestinationRoomId,
        )
        assertEquals("broome-library", state.destinationId)
        assertEquals("Learning Resource Center (LRC)", state.destinationContext)
        assertEquals(room, state.destinationRoom)

        val outdoorRouteState = WalkingRouteState()
        assertFalse(outdoorRouteState.routeRequested)

        val graph = assertNotNull(BroomeIndoorRoutes.current())
        val stairs = assertNotNull(graph.routeFromOutside(room.id, avoidStairs = false, avoidElevators = true))
        val elevator = assertNotNull(graph.routeFromOutside(room.id, avoidStairs = true, avoidElevators = false))
        assertEquals(room.id, stairs.nodes.last().roomId)
        assertEquals(room.id, elevator.nodes.last().roomId)
        assertTrue(stairs.edges.any { it.connection == IndoorConnection.Stairs })
        assertTrue(elevator.edges.any { it.connection == IndoorConnection.Elevator })
        assertTrue(stairs.floorPlanIds.contains("broome-library-first-floor-draft"))
        assertTrue(stairs.floorPlanIds.contains(room.floorPlanId))
        assertTrue(elevator.floorPlanIds.contains("broome-library-first-floor-draft"))
        assertTrue(elevator.floorPlanIds.contains(room.floorPlanId))

        // Browsing back to the outdoor map does not clear the resource destination.
        val returnedToMap = state.preview("broome-library")
        assertEquals(room, returnedToMap.destinationRoom)
        assertEquals(state.destinationContext, returnedToMap.destinationContext)
    }

    @Test
    fun selectingAnotherResourceDestinationDoesNotKeepTheLrcRoom() {
        val lrc = assertNotNull(CampusResourceCatalog.find("learning-resource-center"))
        val state = CampusNavigationState()
            .selectResourceDestination(lrc.directionsBuildingId!!, lrc.name, lrc.indoorDestinationRoomId)
            .selectResourceDestination("sierra-hall", "Another campus resource")

        assertEquals("sierra-hall", state.destinationId)
        assertEquals("Another campus resource", state.destinationContext)
        assertNull(state.destinationRoomId)
        assertNull(state.destinationRoom)
    }

    @Test
    fun normalClassroomDestinationSelectionStillUsesTheExistingRoomDirectory() {
        val classroom = assertNotNull(RoomDirectory.find("broome-library", "broome-library-2330"))
        val state = CampusNavigationState().selectRoomDestination(classroom)

        assertEquals("broome-library", state.destinationId)
        assertEquals(classroom, state.destinationRoom)
        assertEquals("broome-library-second-floor-draft", state.destinationRoom?.floorPlanId)
        assertNull(state.destinationContext)
    }
}
