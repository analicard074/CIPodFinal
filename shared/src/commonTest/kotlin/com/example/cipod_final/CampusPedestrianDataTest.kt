package com.example.cipod_final

import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.routing.CampusPedestrianData
import com.example.cipod_final.routing.GeoPoint
import com.example.cipod_final.routing.PedestrianRoutePlanner
import com.example.cipod_final.routing.PedestrianRouteResult
import com.example.cipod_final.routing.StepFreeStatus
import com.example.cipod_final.ui.map.routeGeoJson
import com.example.cipod_final.ui.map.routeBounds
import com.example.cipod_final.ui.map.destinationGeoJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CampusPedestrianDataTest {
    private val now = 1_000_000L
    private val network = CampusPedestrianData.current

    private fun reading(latitude: Double, longitude: Double) =
        LocationReading(latitude, longitude, accuracyMeters = 5.0, capturedAtMillis = now, precisePermission = true)

    @Test
    fun supportedWalkwayStartsIncludingGatewayAreaReachTheBroomeApproach() {
        val starts = listOf(
            // On the connected footway nearest the Gateway Hall marker (the marker itself is outside 20 m).
            reading(34.1649277, -119.0448354),
            // On a campus footway by Sierra Hall.
            reading(34.162230579435274, -119.04452970944918),
            // The supplied Google Maps point lies on the Broome entrance plaza footway.
            reading(34.162577, -119.041431)
        )

        starts.forEach { start ->
            val route = assertIs<PedestrianRouteResult.Available>(
                PedestrianRoutePlanner.route(start, now, "broome-library", network)
            )
            assertEquals(34.1626619, route.points.last().latitude, 0.0000001)
            assertEquals(-119.0410829, route.points.last().longitude, 0.0000001)
            assertTrue(route.distanceMeters > 0.0)
            assertEquals(false, route.accessibilityVerified)
        }
    }

    @Test
    fun sierraCandidatesMatchOnlyExistingFootwayApproachNodesAndKeepDuplicateSourceIdsDistinct() {
        val candidates = CampusPedestrianData.sierraEntranceCandidates
        assertEquals(6, candidates.size)
        assertEquals(6, candidates.map { it.localId }.distinct().size)
        assertEquals(2, candidates.count { it.recordId == "sierra-northwest-entrance" })

        val expectedMeters = listOf(0.69, 0.11, 0.41, 10.19, 11.33, 3.70)
        candidates.zip(expectedMeters).forEach { (candidate, expected) ->
            assertEquals(expected, candidate.nearestWalkwayMeters, 0.02)
            assertEquals(
                expected,
                PedestrianRoutePlanner.nearestMappedWalkwayDistance(candidate.point, network)!!,
                0.02
            )
        }
        assertEquals(
            listOf("osm-node-12599537566", "osm-node-12072193333", "osm-node-6365428169"),
            candidates.mapNotNull { it.approachNodeId }
        )
        val enabledNodeIds = network.entrances.filter { it.buildingId == "sierra-hall" }.map { it.nodeId }
        assertEquals(candidates.mapNotNull { it.approachNodeId }.toSet(), enabledNodeIds.toSet())
        assertTrue(network.entrances.filter { it.buildingId == "sierra-hall" }
            .all { it.stepFree == StepFreeStatus.UNKNOWN })
    }

    @Test
    fun supportedSierraApproachesAreConnectedAndRouteFromCampusWalkways() {
        val starts = listOf(
            reading(34.1649277, -119.0448354), // Gateway-area supported footway
            reading(34.162230579435274, -119.04452970944918), // Sierra-area footway
            reading(34.162577, -119.041431) // Broome plaza footway
        )
        val enabledNodes = network.entrances.filter { it.buildingId == "sierra-hall" }.map { it.nodeId }.toSet()
        starts.forEach { start ->
            val route = assertIs<PedestrianRouteResult.Available>(
                PedestrianRoutePlanner.route(start, now, "sierra-hall", network)
            )
            val endpoint = network.nodes.single { it.point == route.points.last() }
            assertTrue(endpoint.id in enabledNodes)
            assertTrue(route.distanceMeters > 0.0)
            assertEquals(false, route.accessibilityVerified)
        }
    }

    @Test
    fun unsupportedSierraCandidatesDoNotBecomeRouteEndpoints() {
        assertTrue(CampusPedestrianData.sierraEntranceCandidates.filter { it.approachNodeId == null }
            .all { candidate -> network.entrances.none {
                it.buildingId == "sierra-hall" && network.nodes.single { node -> node.id == it.nodeId }.point == candidate.point
            } })
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ENTRANCE_NOT_VERIFIED),
            PedestrianRoutePlanner.route(reading(34.1649277, -119.0448354), now, "unverified-building", network)
        )
    }

    @Test
    fun sierraRouteGeoJsonEndsAtAReviewedOsmApproachNode() {
        val route = assertIs<PedestrianRouteResult.Available>(
            PedestrianRoutePlanner.route(reading(34.1649277, -119.0448354), now, "sierra-hall", network)
        )
        val last = route.points.last()
        val geoJson = routeGeoJson(route.points)
        assertTrue(geoJson.contains("\"type\":\"LineString\""))
        assertTrue(geoJson.contains("[${last.longitude},${last.latitude}]") ||
            geoJson.contains("[${last.longitude},${last.latitude},"))
    }

    @Test
    fun suppliedPointIsSnappedToTheMappedPlazaFootwayWithoutInventingAConnector() {
        val start = reading(34.162577, -119.041431)
        val route = assertIs<PedestrianRouteResult.Available>(
            PedestrianRoutePlanner.route(start, now, "broome-library", network)
        )

        assertTrue(route.points.first().let {
            kotlin.math.abs(it.latitude - start.latitude) < 0.00001 &&
                kotlin.math.abs(it.longitude - start.longitude) < 0.00001
        })
        assertEquals(34.1626619, route.points.last().latitude, 0.0000001)
        assertEquals(-119.0410829, route.points.last().longitude, 0.0000001)
    }

    @Test
    fun gatewayBuildingMarkerRemainsUnavailableBeyondTheTwentyMeterSnapLimit() {
        val marker = reading(34.164838, -119.045057)
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.NO_NEARBY_WALKWAY),
            PedestrianRoutePlanner.route(marker, now, "broome-library", network)
        )
    }

    @Test
    fun accessibleRouteRemainsUnavailableWithoutVerifiedStepFreeEvidence() {
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED),
            PedestrianRoutePlanner.route(
                reading(34.1649277, -119.0448354), now, "broome-library", network,
                accessibilityRequested = true
            )
        )
        assertTrue(network.edges.all { it.stepFree == StepFreeStatus.UNKNOWN })
    }

    @Test
    fun productionRouteSerializesAsMapLibreLineStringWithLongitudeFirstCoordinates() {
        val route = assertIs<PedestrianRouteResult.Available>(
            PedestrianRoutePlanner.route(
                reading(34.1649277, -119.0448354), now, "broome-library", network
            )
        )

        val geoJson = routeGeoJson(route.points)
        assertTrue(geoJson.contains("\"type\":\"FeatureCollection\""))
        assertTrue(geoJson.contains("\"type\":\"LineString\""))
        assertTrue(geoJson.contains("[-119.0410829,34.1626619]"))
        assertEquals(
            "-119.0448354,34.1649277",
            geoJson.substringAfter("\"coordinates\":[[").substringBefore("]")
        )
    }

    @Test
    fun routeCameraBoundsIncludeTheStartAndEntranceEndpointWithoutChangingGeometry() {
        val route = assertIs<PedestrianRouteResult.Available>(
            PedestrianRoutePlanner.route(
                reading(34.1649277, -119.0448354), now, "broome-library", network
            )
        )
        val bounds = routeBounds(route.points)

        assertEquals(route.points.minOf { it.longitude }, bounds.southwest.longitude)
        assertEquals(route.points.minOf { it.latitude }, bounds.southwest.latitude)
        assertEquals(route.points.maxOf { it.longitude }, bounds.northeast.longitude)
        assertEquals(route.points.maxOf { it.latitude }, bounds.northeast.latitude)
        assertTrue(routeGeoJson(route.points).contains("[-119.0448354,34.1649277"))
        assertEquals(
            """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[-119.0410829,34.1626619]}}]}""",
            destinationGeoJson(route.points.last())
        )
    }

    @Test
    fun productionCoverageRejectsUnsupportedPositions() {
        assertTrue(network.nodes.all { network.covers(it.point) })
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.OUTSIDE_COVERAGE),
            PedestrianRoutePlanner.route(reading(34.168, -119.045), now, "broome-library", network)
        )
    }
}
