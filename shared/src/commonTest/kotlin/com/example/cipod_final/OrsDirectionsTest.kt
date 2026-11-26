package com.example.cipod_final

import com.example.cipod_final.buildings.BuildingCoordinates
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.routing.*
import com.example.cipod_final.routing.OrsDirections
import com.example.cipod_final.routing.RouteFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*

class OrsDirectionsTest {
    private val start = BuildingCoordinates(34.162, -119.0435)
    private val end = BuildingCoordinates(34.1645, -119.0412)
    private val route = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[[-119.0435,34.162],[-119.0412,34.1645]]}}]}"""

    @Test fun requestsPedestrianPreferredRoutingWithFullPathGeometry() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://api.heigit.org/openrouteservice/v2/directions/foot-walking/geojson", request.url.toString())
            assertEquals("test-key", request.headers[HttpHeaders.Authorization])
            assertEquals(ContentType.Application.Json, request.body.contentType)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("recommended", body.getValue("preference").jsonPrimitive.content)
            assertFalse(body.getValue("geometry_simplify").jsonPrimitive.boolean)
            assertEquals(listOf(25, 25), body.getValue("radiuses").jsonArray.map { it.jsonPrimitive.int })
            val coordinates = body.getValue("coordinates").jsonArray
            assertEquals(-119.0435, coordinates[0].jsonArray[0].jsonPrimitive.double)
            assertEquals(34.162, coordinates[0].jsonArray[1].jsonPrimitive.double)
            assertEquals(-119.0412, coordinates[1].jsonArray[0].jsonPrimitive.double)
            respond(route, HttpStatusCode.OK)
        })
        try { assertEquals(route, OrsDirections(client, "test-key").walkingRoute(start, end)) }
        finally { client.close() }
    }

    @Test fun sierraToBroomeUsesTheServicePathForBuildingAndNearbyGpsStarts() = runTest {
        val sierra = assertNotNull(CampusWalkingAccess.forBuilding("sierra-hall"))
        val eastExit = assertNotNull(CampusWalkingAccess.departureFor("sierra-hall", "broome-library"))
        assertEquals(BuildingCoordinates(34.1622379, -119.0443425), eastExit)
        val broome = assertNotNull(CampusWalkingAccess.forBuilding("broome-library"))
        for (origin in listOf(eastExit, sierra.copy(latitude = sierra.latitude + .00001))) {
            // Synthetic path bends verify that the service response is kept, not a fixed avenue route.
            val response = """
                {"type":"FeatureCollection","features":[{
                    "type":"Feature","properties":{},
                    "geometry":{"type":"LineString","coordinates":[
                        [${origin.longitude},${origin.latitude}],
                        [-119.0438,34.16235],[-119.0437,34.16235],
                        [${broome.longitude},${broome.latitude}]
                    ]}
                }]}
            """.trimIndent()
            var requests = 0
            val client = HttpClient(MockEngine { request ->
                requests++
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                val coordinates = body.getValue("coordinates").jsonArray
                assertEquals(2, coordinates.size, "Send only the start and entrance, without forced street waypoints")
                assertEquals(origin.longitude, coordinates.first().jsonArray[0].jsonPrimitive.double)
                assertEquals(origin.latitude, coordinates.first().jsonArray[1].jsonPrimitive.double)
                assertEquals(broome.longitude, coordinates.last().jsonArray[0].jsonPrimitive.double)
                assertEquals(broome.latitude, coordinates.last().jsonArray[1].jsonPrimitive.double)
                assertEquals("recommended", body.getValue("preference").jsonPrimitive.content)
                assertFalse(body.getValue("geometry_simplify").jsonPrimitive.boolean)
                respond(response, HttpStatusCode.OK)
            })
            try {
                val raw = OrsDirections(client, "test-key").walkingRoute(origin, broome)
                val details = walkingRouteDetails(raw, origin, broome)
                assertEquals(1, requests)
                assertEquals(listOf(origin, BuildingCoordinates(34.16235, -119.0438),
                    BuildingCoordinates(34.16235, -119.0437), broome),
                    details.points.map { BuildingCoordinates(it.latitude, it.longitude) })
                assertEquals(details.geoJson, details.displayGeoJson)
            } finally {
                client.close()
            }
        }
    }

    @Test fun eastExitOnlyChangesDeparturesFromSierraTowardBroome() {
        val sierraMain = CampusWalkingAccess.forBuilding("sierra-hall")
        assertEquals(BuildingCoordinates(34.1621084, -119.0444844), sierraMain)
        assertEquals(sierraMain, CampusWalkingAccess.departureFor("sierra-hall", "gateway-hall"))
        assertEquals(CampusWalkingAccess.forBuilding("broome-library"),
            CampusWalkingAccess.departureFor("broome-library", "sierra-hall"))
        assertNull(CampusWalkingAccess.departureFor("unknown", "broome-library"))
    }

    @Test fun missingKeyDoesNotSendRequest() = runTest {
        val client = HttpClient(MockEngine { error("No request expected") })
        try { assertFailsWith<RouteFailure> { OrsDirections(client, "").walkingRoute(start, end) } }
        finally { client.close() }
    }

    @Test fun identicalEndpointsDoNotSendRequest() = runTest {
        val client = HttpClient(MockEngine { error("No request expected") })
        try { assertFailsWith<RouteFailure> { OrsDirections(client, "test-key").walkingRoute(start, start) } }
        finally { client.close() }
    }

    @Test fun serviceErrorsDoNotExposeResponseBody() = runTest {
        for (status in listOf(400, 401, 403, 404, 429, 500)) {
            val client = HttpClient(MockEngine { respond("sensitive response", HttpStatusCode.fromValue(status)) })
            try {
                val failure = assertFailsWith<RouteFailure> { OrsDirections(client, "test-key").walkingRoute(start, end) }
                assertFalse(failure.userMessage.contains("sensitive"))
            } finally { client.close() }
        }
    }

    @Test fun rejectsEmptyMalformedAndNonLineResponses() = runTest {
        for (body in listOf("not json", """{"type":"FeatureCollection","features":[]}""", """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[0,0]}}]}""")) {
            val client = HttpClient(MockEngine { respond(body, HttpStatusCode.OK) })
            try { assertFailsWith<RouteFailure> { OrsDirections(client, "test-key").walkingRoute(start, end) } }
            finally { client.close() }
        }
    }
}

class WalkingRouteDetailsTest {
    private val start = BuildingCoordinates(34.162, -119.044)
    private val bend = BuildingCoordinates(34.1624, -119.042)
    private val entrance = assertNotNull(CampusWalkingAccess.forBuilding("broome-library"))
    private fun response(points: List<BuildingCoordinates>, summary: Boolean = true): String = buildJsonObject {
        put("type", "FeatureCollection")
        putJsonArray("features") {
            addJsonObject {
                put("type", "Feature")
                putJsonObject("properties") {
                    if (summary) putJsonObject("summary") { put("distance", 320); put("duration", 240) }
                }
                putJsonObject("geometry") {
                    put("type", "LineString")
                    putJsonArray("coordinates") { points.forEach { point ->
                        addJsonArray { add(point.longitude); add(point.latitude) }
                    } }
                }
            }
        }
    }.toString()

    @Test fun usesProviderTripSummaryAndIncludesEveryPointInBounds() {
        val route = walkingRouteDetails(response(listOf(start, bend, entrance)), start, entrance)
        assertEquals("4 min walk · 320 m", route.tripLabel)
        assertEquals("4 min", route.durationLabel)
        assertEquals("320 m", route.distanceLabel)
        assertEquals(3, route.points.size)
        assertEquals(bend.longitude, route.points[1].longitude)
        route.points.forEach { point ->
            assertTrue(point.longitude in route.bounds.west..route.bounds.east)
            assertTrue(point.latitude in route.bounds.south..route.bounds.north)
        }
    }

    @Test fun anchorsEndpointsToRequestedStartAndEntranceWithoutLosingBends() {
        val snappedStart = start.copy(latitude = start.latitude + .00003)
        val snappedEnd = entrance.copy(latitude = entrance.latitude + .00005)
        val route = walkingRouteDetails(response(listOf(snappedStart, bend, snappedEnd)), start, entrance)
        val coordinates = Json.parseToJsonElement(route.geoJson).jsonObject.getValue("features").jsonArray[0]
            .jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray
        assertEquals(start.latitude, coordinates.first().jsonArray[1].jsonPrimitive.double)
        assertEquals(entrance.latitude, coordinates.last().jsonArray[1].jsonPrimitive.double)
        assertEquals(entrance.longitude, coordinates.last().jsonArray[0].jsonPrimitive.double)
        assertEquals(bend.longitude, coordinates[1].jsonArray[0].jsonPrimitive.double)
        assertTrue(route.distanceMeters > 0 && route.durationSeconds > 0)
    }

    @Test fun rejectsDistantNetworkSnapsAndInvalidGeometry() {
        val distant = entrance.copy(latitude = entrance.latitude + .001)
        assertFailsWith<RouteFailure> { walkingRouteDetails(response(listOf(start, bend, distant)), start, entrance) }
        assertFailsWith<RouteFailure> { walkingRouteDetails(response(listOf(entrance)), start, entrance) }
        assertFailsWith<RouteFailure> { walkingRouteDetails("{}", start, entrance) }
    }

    @Test fun missingSummaryUsesPathLengthAndAnEstimatedWalkingTime() {
        val route = walkingRouteDetails(response(listOf(start, bend, entrance), summary = false), start, entrance)
        val length = distanceMeters(start, bend) + distanceMeters(bend, entrance)
        assertEquals(length, route.distanceMeters, .001)
        assertEquals(length / 1.4, route.durationSeconds, .001)
    }

    private fun corridorPoint(x: Double, y: Double) = BuildingCoordinates(
        start.latitude + y / 111195,
        start.longitude + x / (111195 * cos(start.latitude * PI / 180)),
    )

    @Test fun displayKeepsEveryPedestrianBendAndEndpointApproach() {
        val points = listOf(
            corridorPoint(0.0, 0.0), corridorPoint(0.0, 10.0),
            corridorPoint(20.0, 10.0), corridorPoint(20.0, 15.0),
            corridorPoint(30.0, 15.0), corridorPoint(30.0, 10.0),
            corridorPoint(60.0, 10.0), corridorPoint(60.0, 40.0),
            corridorPoint(90.0, 40.0), corridorPoint(90.0, 50.0),
        )
        val route = walkingRouteDetails(response(points), points.first(), points.last())
        val display = Json.parseToJsonElement(route.displayGeoJson).jsonObject.getValue("features").jsonArray[0]
            .jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray
        val displayedPoints = display.map {
            BuildingCoordinates(it.jsonArray[1].jsonPrimitive.double, it.jsonArray[0].jsonPrimitive.double)
        }
        assertEquals(points, displayedPoints)
        assertEquals(route.geoJson, route.displayGeoJson)
        assertEquals(points.size, route.points.size)
        assertEquals(320.0, route.distanceMeters)
        assertEquals(240.0, route.durationSeconds)
    }

    @Test fun shortApproachesAndDuplicateVerticesAreRetained() {
        val shortPath = listOf(corridorPoint(0.0, 0.0), corridorPoint(1.0, 1.0),
            corridorPoint(1.0, 1.0), corridorPoint(2.0, 2.0))
        for (points in listOf(shortPath, List(6) { start })) {
            val route = walkingRouteDetails(response(points), points.first(), points.last())
            val actual = route.points.map { BuildingCoordinates(it.latitude, it.longitude) }
            assertEquals(points, actual)
            assertEquals(route.geoJson, route.displayGeoJson)
        }
    }

    @Test fun entranceReadinessRequiresFreshAccurateLocationAndAccountsForUncertainty() {
        val now = 10_000_000L
        val here = LocationReading(entrance.latitude, entrance.longitude, 5.0, now, true)
        assertTrue(nearEntrance(here, entrance, now))
        assertFalse(nearEntrance(null, entrance, now))
        assertFalse(nearEntrance(here, null, now))
        assertFalse(nearEntrance(here.copy(capturedAtMillis = now - LocationQualityPolicy.maxAgeMillis), entrance, now))
        assertFalse(nearEntrance(here.copy(accuracyMeters = 100.0), entrance, now))
        assertFalse(nearEntrance(here.copy(precisePermission = false), entrance, now))
        assertFalse(nearEntrance(here.copy(latitude = entrance.latitude + .001), entrance, now))
        // Arrival requires the accuracy radius to fit inside the 25 m entrance area.
        assertTrue(nearEntrance(here.copy(latitude = entrance.latitude + .00017), entrance, now))
        assertFalse(nearEntrance(here.copy(latitude = entrance.latitude + .00020), entrance, now))
        assertFalse(nearEntrance(here.copy(latitude = entrance.latitude + .0003, accuracyMeters = 25.0), entrance, now))
    }
}
