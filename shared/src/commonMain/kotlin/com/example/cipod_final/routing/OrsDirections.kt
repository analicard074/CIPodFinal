package com.example.cipod_final.routing

import com.example.cipod_final.buildings.BuildingCoordinates
import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationReading
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
                    require(
                        point.latitude.isFinite() && point.latitude in -90.0..90.0 &&
                                point.longitude.isFinite() && point.longitude in -180.0..180.0
                    )
                    addJsonArray { add(point.longitude); add(point.latitude) }
                }
            }
            // Prefer pedestrian paths over street shortcuts, keeping sidewalk bends intact.
            put("preference", "recommended")
            put("geometry_simplify", false)
            // Fail if the pedestrian network is too far away rather than silently jumping streets.
            putJsonArray("radiuses") { add(25); add(25) }
            put("instructions", false)
        }
        val response =
            client.post("https://api.heigit.org/openrouteservice/v2/directions/foot-walking/geojson") {
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

internal fun distanceMeters(a: BuildingCoordinates, b: BuildingCoordinates): Double {
    // Haversine formula: distance between two latitude/longitude points on Earth.
    val lat1 = a.latitude * PI / 180
    val lat2 = b.latitude * PI / 180
    val deltaLat = lat2 - lat1
    val deltaLon = (b.longitude - a.longitude) * PI / 180
    val h = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    return 6_371_000 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

internal fun nearEntrance(
    reading: LocationReading?,
    entrance: BuildingCoordinates?,
    nowMillis: Long
): Boolean {
    if (reading == null || entrance == null) return false
    if (LocationQualityPolicy.evaluate(reading, nowMillis) != LocationQuality.Usable) return false
    val position = BuildingCoordinates(reading.latitude, reading.longitude)
    return distanceMeters(position, entrance) + reading.accuracyMeters <= 25
}

internal data class WalkingRouteDetails(
    val geoJson: String,
    val points: List<Position>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val displayGeoJson: String = geoJson,
    val estimatedTime: Boolean = false,
    val attribution: String = "© openrouteservice | © OpenStreetMap contributors",
    val destinationName: String = "",
    val entranceLabel: String = "Entrance",
) {
    val bounds: BoundingBox
        get() = BoundingBox(
            west = points.minOf { it.longitude } - .00001,
            south = points.minOf { it.latitude } - .00001,
            east = points.maxOf { it.longitude } + .00001,
            north = points.maxOf { it.latitude } + .00001,
        )
    val durationLabel: String get() = "${ceil(durationSeconds / 60).toInt().coerceAtLeast(1)} min"
    val distanceLabel: String
        get() = if (distanceMeters < 1000) "${distanceMeters.roundToInt()} m"
        else "${(distanceMeters / 100).roundToInt() / 10.0} km"
    val tripLabel: String get() = "${if (estimatedTime) "About " else ""}$durationLabel walk · $distanceLabel"
}

// Keep the provider's path bends, but anchor the first/last segment to the requested endpoints.
internal fun walkingRouteDetails(
    raw: String,
    start: BuildingCoordinates,
    entrance: BuildingCoordinates
): WalkingRouteDetails {
    try {
        val root = Json.parseToJsonElement(raw).jsonObject
        val feature = root.getValue("features").jsonArray.first { element ->
            element.jsonObject["geometry"]?.jsonObject?.get("type")?.jsonPrimitive?.content == "LineString"
        }.jsonObject
        val coordinates = feature.getValue("geometry").jsonObject.getValue("coordinates").jsonArray
        val original = coordinates.map { value ->
            val pair = value.jsonArray
            val lon = pair[0].jsonPrimitive.double
            val lat = pair[1].jsonPrimitive.double
            require(lon.isFinite() && lon in -180.0..180.0 && lat.isFinite() && lat in -90.0..90.0)
            BuildingCoordinates(lat, lon)
        }
        require(original.size >= 2)
        // Do not invent a connector across an unmapped gap beyond the request's snap radius.
        if (distanceMeters(original.first(), start) > 25 || distanceMeters(
                original.last(),
                entrance
            ) > 25
        ) {
            throw RouteFailure("The walking path does not reach the selected entrance. Try another starting point.")
        }
        val adjusted = original.toMutableList()
        adjusted[0] = start
        adjusted[adjusted.lastIndex] = entrance
        fun length(points: List<BuildingCoordinates>) =
            points.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) }

        val summary = feature["properties"]?.jsonObject?.get("summary")?.jsonObject
        fun metric(name: String): Double? = summary?.get(name)?.jsonPrimitive?.doubleOrNull
            ?.takeIf { it.isFinite() && it >= 0 }

        val originalLength = length(original)
        val extraDistance = length(adjusted) - originalLength
        val distance = ((metric("distance") ?: originalLength) + extraDistance).coerceAtLeast(0.0)
        val duration = ((metric("duration")
            ?: (originalLength / 1.4)) + extraDistance / 1.4).coerceAtLeast(0.0)
        return WalkingRouteDetails(
            geoJson = routeGeoJson(adjusted, feature),
            points = adjusted.map { Position(it.longitude, it.latitude) },
            distanceMeters = distance,
            durationSeconds = duration,
        )
    } catch (failure: RouteFailure) {
        throw failure
    } catch (_: Exception) {
        throw RouteFailure("Routing service returned an invalid route. Retry later.")
    }
}

// Keep all returned path points when drawing the adjusted route.
private fun routeGeoJson(
    points: List<BuildingCoordinates>,
    feature: JsonObject,
): String {
    val geometry = buildJsonObject {
        put("type", "LineString")
        putJsonArray("coordinates") {
            for (point in points) {
                addJsonArray {
                    add(point.longitude)
                    add(point.latitude)
                }
            }
        }
    }
    return buildJsonObject {
        put("type", "FeatureCollection")
        putJsonArray("features") { add(JsonObject(feature + ("geometry" to geometry))) }
    }.toString()
}
