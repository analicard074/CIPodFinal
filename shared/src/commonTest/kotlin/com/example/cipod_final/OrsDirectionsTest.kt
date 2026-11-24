package com.example.cipod_final

import com.example.cipod_final.buildings.BuildingCoordinates
import com.example.cipod_final.routing.OrsDirections
import com.example.cipod_final.routing.RouteFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class OrsDirectionsTest {
    private val start = BuildingCoordinates(34.162, -119.0435)
    private val end = BuildingCoordinates(34.1645, -119.0412)
    private val route = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[[-119.0435,34.162],[-119.0412,34.1645]]}}]}"""

    @Test fun postsAuthorizedLongitudeFirstGeoJsonRequest() = runTest {
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
