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

    @Test fun petitSalonIsSearchableAtTheOfficialCampusMapMarker() {
        val petitSalon = CampusBuildings.search("  petit salon ").single()
        assertEquals("petit-salon", petitSalon.id)
        assertEquals(34.163609, petitSalon.coordinates?.latitude)
        assertEquals(-119.043770, petitSalon.coordinates?.longitude)
        assertTrue(petitSalon.universitySource.contains("maps.csuci.edu"))
        assertEquals("petit-salon", CampusNavigationState().selectDestination(petitSalon.id).destination?.id)
    }

    @Test fun gatewayRemainsSearchableWithItsExistingMapMarker() {
        val gateway = CampusBuildings.search("gateway").single()
        assertEquals("gateway-hall", gateway.id)
        assertTrue(gateway.coordinates != null)
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

    @Test fun unknownDestinationCannotReplaceExistingChoice() {
        val state = CampusNavigationState().selectDestination("sierra-hall")
        assertEquals("gateway-hall", state.selectDestination("gateway-hall").destination?.id)
        assertEquals(state, state.selectDestination("missing"))
        assertEquals(state, state.preview("missing"))
    }

    @Test fun selectingSameDestinationIsIdempotent() {
        val state = CampusNavigationState().selectDestination("broome-library")
        assertEquals(state, state.selectDestination("broome-library"))
    }

    @Test fun resourceDestinationPreservesContextAndClearingRemovesIt() {
        val state = CampusNavigationState().selectResourceDestination(
            "broome-library",
            "Learning Resource Center (LRC)",
            "broome-library-2730",
        )
        assertEquals("broome-library", state.destination?.id)
        assertEquals("Learning Resource Center (LRC)", state.destinationContext)
        assertEquals("broome-library-2730", state.destinationRoomId)

        val cleared = state.clearDestination()
        assertNull(cleared.destination)
        assertNull(cleared.destinationContext)
    }

    @Test fun regularDestinationSelectionClearsPriorResourceContext() {
        val state = CampusNavigationState()
            .selectResourceDestination("broome-library", "Learning Resource Center")
            .selectDestination("sierra-hall")
        assertEquals("sierra-hall", state.destination?.id)
        assertNull(state.destinationContext)
    }
}
