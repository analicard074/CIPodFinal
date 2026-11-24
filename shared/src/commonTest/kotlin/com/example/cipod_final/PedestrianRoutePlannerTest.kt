package com.example.cipod_final

import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.routing.GeoPoint
import com.example.cipod_final.routing.PedestrianNetwork
import com.example.cipod_final.routing.PedestrianRoutePlanner
import com.example.cipod_final.routing.PedestrianRouteResult
import com.example.cipod_final.routing.StepFreeStatus
import com.example.cipod_final.routing.VerifiedEntrance
import com.example.cipod_final.routing.WalkwayEdge
import com.example.cipod_final.routing.WalkwayNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PedestrianRoutePlannerTest {
    private val now = 1_000_000L
    private val reading = LocationReading(34.0, -119.0, 5.0, now, precisePermission = true)

    private fun network(stepFree: StepFreeStatus = StepFreeStatus.UNKNOWN) = PedestrianNetwork(
        nodes = listOf(
            WalkwayNode("a", GeoPoint(34.0, -119.0)),
            WalkwayNode("b", GeoPoint(34.0001, -119.0)),
            WalkwayNode("c", GeoPoint(34.0002, -119.0)),
            WalkwayNode("d", GeoPoint(34.0001, -118.9999))
        ),
        edges = listOf(
            WalkwayEdge("ab", "a", "b", stepFree),
            WalkwayEdge("bc", "b", "c", stepFree),
            WalkwayEdge("bd", "b", "d", stepFree),
            WalkwayEdge("dc", "d", "c", stepFree)
        ),
        entrances = listOf(VerifiedEntrance("broome-library", "c", "test survey", stepFree)),
        sourceAttribution = "test-only fixture",
        coverageBoundary = listOf(
            GeoPoint(33.9995, -119.0005), GeoPoint(33.9995, -118.9995),
            GeoPoint(34.0005, -118.9995), GeoPoint(34.0005, -119.0005)
        )
    )

    @Test
    fun shortestConnectedRouteSnapsToMappedEdgeAndEndsAtVerifiedEntrance() {
        val result = PedestrianRoutePlanner.route(reading, now, "broome-library", network())
        val route = assertIs<PedestrianRouteResult.Available>(result)
        assertEquals(3, route.points.size)
        assertEquals(34.0, route.points.first().latitude, 0.000001)
        assertEquals(34.0002, route.points.last().latitude, 0.000001)
        // Dijkstra chooses the direct b-c edge over the two-edge b-d-c detour.
        assertEquals(34.0001, route.points[1].latitude, 0.000001)
        assertEquals(false, route.accessibilityVerified)
    }

    @Test
    fun choosesShortestConnectedEntranceEvenWhenAnotherEntranceIsGeographicallyCloser() {
        val multipleEntrances = network().copy(
            nodes = listOf(
                WalkwayNode("a", GeoPoint(34.0, -119.0)),
                WalkwayNode("b", GeoPoint(34.0001, -119.0)),
                WalkwayNode("e", GeoPoint(34.0001, -118.9997)),
                WalkwayNode("f", GeoPoint(34.0001, -119.0003)),
                WalkwayNode("c", GeoPoint(34.00001, -119.00001)),
                WalkwayNode("d", GeoPoint(34.0001, -118.9999))
            ),
            edges = listOf(
                WalkwayEdge("ab", "a", "b"),
                WalkwayEdge("be", "b", "e"),
                WalkwayEdge("ef", "e", "f"),
                WalkwayEdge("fc", "f", "c"),
                WalkwayEdge("bd", "b", "d")
            ),
            entrances = listOf(
                VerifiedEntrance("sierra-hall", "c", "geographically close but longer connected path"),
                VerifiedEntrance("sierra-hall", "d", "farther straight-line but shorter connected path")
            )
        )
        val route = assertIs<PedestrianRouteResult.Available>(
            PedestrianRoutePlanner.route(reading, now, "sierra-hall", multipleEntrances)
        )
        assertEquals(34.0001, route.points.last().latitude, 0.000001)
        assertEquals(-118.9999, route.points.last().longitude, 0.000001)
    }

    @Test
    fun staleAndApproximateLocationsAreRejectedBeforeSnapping() {
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.LOCATION_NOT_USABLE),
            PedestrianRoutePlanner.route(reading.copy(capturedAtMillis = now - 120_000), now, "broome-library", network())
        )
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.LOCATION_NOT_USABLE),
            PedestrianRoutePlanner.route(reading.copy(precisePermission = false), now, "broome-library", network())
        )
    }

    @Test
    fun rejectsLocationsBeyondConservativeWalkwaySnapTolerance() {
        val far = reading.copy(longitude = reading.longitude + 0.0004)
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.NO_NEARBY_WALKWAY),
            PedestrianRoutePlanner.route(far, now, "broome-library", network())
        )
    }

    @Test
    fun rejectsLocationsOutsideTheDeclaredNetworkCoveragePolygon() {
        val outside = reading.copy(latitude = 34.001)
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.OUTSIDE_COVERAGE),
            PedestrianRoutePlanner.route(outside, now, "broome-library", network())
        )
    }

    @Test
    fun disconnectedWalkwayGraphReturnsUnavailableWithoutConnectingTheGap() {
        val disconnected = network().copy(edges = listOf(WalkwayEdge("ab", "a", "b")))
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.NO_CONNECTED_ROUTE),
            PedestrianRoutePlanner.route(reading, now, "broome-library", disconnected)
        )
    }

    @Test
    fun accessibilityRequiresEveryUsedSegmentAndEntranceVerifiedStepFree() {
        val unknown = PedestrianRoutePlanner.route(reading, now, "broome-library", network(), true)
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED),
            unknown
        )
        val verified = PedestrianRoutePlanner.route(
            reading, now, "broome-library", network(StepFreeStatus.VERIFIED_STEP_FREE), true
        )
        assertIs<PedestrianRouteResult.Available>(verified).also { assertEquals(true, it.accessibilityVerified) }
    }

    @Test
    fun manualBuildingStartRequiresItsOwnVerifiedEntrance() {
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ENTRANCE_NOT_VERIFIED),
            PedestrianRoutePlanner.routeFromBuilding("sierra-hall", "broome-library", network())
        )
        val withStartEntrance = network().copy(
            entrances = network().entrances + VerifiedEntrance("sierra-hall", "a", "surveyed entrance")
        )
        assertIs<PedestrianRouteResult.Available>(
            PedestrianRoutePlanner.routeFromBuilding("sierra-hall", "broome-library", withStartEntrance)
        )
    }

    @Test
    fun missingProductionNetworkAndEntranceNeverGenerateAStraightLine() {
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.NETWORK_NOT_VERIFIED),
            PedestrianRoutePlanner.route(reading, now, "broome-library", null)
        )
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ENTRANCE_NOT_VERIFIED),
            PedestrianRoutePlanner.route(reading, now, "other-building", network())
        )
    }

}
