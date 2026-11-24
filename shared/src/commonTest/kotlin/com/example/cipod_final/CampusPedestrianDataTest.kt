package com.example.cipod_final

import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.routing.CampusPedestrianData
import com.example.cipod_final.routing.GeoPoint
import com.example.cipod_final.routing.PedestrianRoutePlanner
import com.example.cipod_final.routing.PedestrianRouteResult
import com.example.cipod_final.routing.StepFreeStatus
import com.example.cipod_final.ui.map.routeGeoJson
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
    fun productionCoverageRejectsUnsupportedPositions() {
        assertTrue(network.nodes.all { network.covers(it.point) })
        assertEquals(
            PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.OUTSIDE_COVERAGE),
            PedestrianRoutePlanner.route(reading(34.168, -119.045), now, "broome-library", network)
        )
    }
}
