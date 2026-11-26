package com.example.cipod_final.routing

import com.example.cipod_final.buildings.BuildingCoordinates
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import org.maplibre.spatialk.geojson.FeatureCollection

internal class RouteFailure(val userMessage: String) : Exception(userMessage)

internal class OrsDirections(
    private val client: HttpClient,
    private val apiKey: String,
) {
    suspend fun walkingRoute(start: BuildingCoordinates, end: BuildingCoordinates): String {
        if (apiKey.isBlank()) throw RouteFailure("Set ors.apiKey in local.properties and rebuild to enable routes.")
        if (start == end) throw RouteFailure("Choose different starting and destination buildings.")
        val body = buildJsonObject {
            putJsonArray("coordinates") {
                listOf(start, end).forEach { point ->
                    require(point.latitude.isFinite() && point.latitude in -90.0..90.0 &&
                        point.longitude.isFinite() && point.longitude in -180.0..180.0)
                    addJsonArray { add(point.longitude); add(point.latitude) }
                }
            }
            // Recommended walking weights favor designated pedestrian ways. Keep all path bends.
            put("preference", "recommended")
            put("geometry_simplify", false)
            // Fail if the pedestrian network is too far away rather than silently jumping streets.
            putJsonArray("radiuses") { add(25); add(25) }
            put("instructions", false)
        }
        val response = client.post("https://api.heigit.org/openrouteservice/v2/directions/foot-walking/geojson") {
            header(HttpHeaders.Authorization, apiKey)
            header(HttpHeaders.Accept, "application/geo+json")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        when (response.status.value) {
            200 -> Unit
            401, 403 -> throw RouteFailure("ORS rejected the API key. Check its access and rebuild.")
            429 -> throw RouteFailure("ORS request limit reached. Try again later.")
            400, 404 -> throw RouteFailure("No nearby walking path found. Refresh your location or choose another starting building.")
            else -> throw RouteFailure("Routing service unavailable. Try again later.")
        }
        val geoJson = response.bodyAsText()
        val collection = try {
            FeatureCollection.fromJson<org.maplibre.spatialk.geojson.Geometry?, JsonObject?>(geoJson)
        } catch (_: Exception) {
            throw RouteFailure("Routing service returned an invalid route. Retry later.")
        }
        if (collection.features.isEmpty() || collection.features.none {
                it.geometry is org.maplibre.spatialk.geojson.LineString
            }) throw RouteFailure("No walking route found for these points.")
        return geoJson
    }
}

internal fun routingHttpClient() = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }
    // Never forward the authorization header to a redirect target.
    followRedirects = false
}
