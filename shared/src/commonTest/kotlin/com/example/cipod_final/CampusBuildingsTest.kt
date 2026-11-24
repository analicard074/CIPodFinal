package com.example.cipod_final

import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.buildings.CampusNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampusBuildingsTest {
    @Test fun blankSearchReturnsDirectory() {
        assertEquals(CampusBuildings.all, CampusBuildings.search("   "))
    }

    @Test fun searchTrimsAndIgnoresCase() {
        assertEquals(listOf("sierra-hall"), CampusBuildings.search("  SiErRa  ").map { it.id })
    }

    @Test fun partialSearchCanMatchSeveralBuildings() {
        assertEquals(listOf("gateway-hall", "sierra-hall"), CampusBuildings.search("hall").map { it.id })
    }

    @Test fun unmatchedSearchIsEmpty() {
        assertTrue(CampusBuildings.search("unknown building").isEmpty())
    }

    @Test fun unverifiedBuildingsRemainSearchableWithoutCoordinates() {
        val gateway = CampusBuildings.search("gateway").single()
        assertNull(gateway.coordinates)
        assertTrue(gateway.locationNote.contains("awaiting verification"))
    }

    @Test fun previewDoesNotSetDestination() {
        val state = CampusNavigationState().preview("sierra-hall")
        assertEquals("sierra-hall", state.selectedBuilding?.id)
        assertNull(state.destination)
    }

    @Test fun destinationCanBeSelectedAndReplaced() {
        val state = CampusNavigationState().selectDestination("sierra-hall")
        assertEquals("sierra-hall", state.destination?.id)
        val replaced = state.selectDestination("broome-library")
        assertEquals("broome-library", replaced.destination?.id)
        assertEquals("broome-library", replaced.selectedBuilding?.id)
        assertNull(replaced.destinationRoomId)
        assertNull(replaced.destinationFloorPlanId)
    }

    @Test fun browsingAndFilteringPreserveDestination() {
        val state = CampusNavigationState().selectDestination("sierra-hall")
            .preview("broome-library").copy(query = "gateway")
        assertEquals("sierra-hall", state.destination?.id)
        assertEquals("broome-library", state.selectedBuilding?.id)
        assertEquals(listOf("gateway-hall"), state.results.map { it.id })
    }

    @Test fun clearingDestinationPreservesPreview() {
        val state = CampusNavigationState().selectDestination("sierra-hall").clearDestination()
        assertNull(state.destination)
        assertEquals("sierra-hall", state.selectedBuilding?.id)
        assertEquals(state, state.clearDestination())
    }

    @Test fun unknownOrUnverifiedDestinationCannotReplaceExistingChoice() {
        val state = CampusNavigationState().selectDestination("sierra-hall")
        assertEquals(state, state.selectDestination("gateway-hall"))
        assertEquals(state, state.selectDestination("missing"))
        assertEquals(state, state.preview("missing"))
    }

    @Test fun selectingBroomeIsBuildingOnlyUntilStudentSelectsARoom() {
        val selected = CampusNavigationState().selectDestination("broome-library")
        assertEquals("broome-library", selected.destination?.id)
        assertNull(selected.destinationRoomId)
        assertNull(selected.destinationFloorPlanId)
    }

    @Test fun selectedRoomAndFloorSurviveReopeningTheSameDestination() {
        val withRoom = CampusNavigationState().selectDestination("broome-library")
            .selectRoom("broome-library-2330", "broome-library-second-floor-draft")

        val reopened = withRoom.selectDestination("broome-library")

        assertEquals("broome-library-2330", reopened.destinationRoomId)
        assertEquals("broome-library-second-floor-draft", reopened.destinationFloorPlanId)
    }

    @Test fun clearingDestinationAlsoClearsRoomAndFloor() {
        val cleared = CampusNavigationState().selectDestination("broome-library")
            .selectRoom("broome-library-2330", "broome-library-second-floor-draft")
            .clearDestination()

        assertNull(cleared.destination)
        assertNull(cleared.destinationRoomId)
        assertNull(cleared.destinationFloorPlanId)
    }

    @Test fun switchingBuildingsDoesNotCarryOverSelectedRoomOrFloor() {
        val switched = CampusNavigationState().selectDestination("broome-library")
            .selectRoom("broome-library-2330", "broome-library-second-floor-draft")
            .selectDestination("sierra-hall")

        assertEquals("sierra-hall", switched.destination?.id)
        assertNull(switched.destinationRoomId)
        assertNull(switched.destinationFloorPlanId)
    }
}
