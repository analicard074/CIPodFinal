package com.example.cipod_final.routing

import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationReading

/** WGS84 point used only by the pedestrian network and route planner. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

enum class StepFreeStatus { VERIFIED_STEP_FREE, VERIFIED_NOT_STEP_FREE, UNKNOWN }

data class WalkwayNode(val id: String, val point: GeoPoint)

data class WalkwayEdge(
    val id: String,
    val from: String,
    val to: String,
    val stepFree: StepFreeStatus = StepFreeStatus.UNKNOWN
)

/**
 * A route endpoint must be source-verified on a connected pedestrian approach, never inferred
 * from a building center. It may mark the exterior entrance approach rather than the door threshold.
 */
data class VerifiedEntrance(
    val buildingId: String,
    val nodeId: String,
    val verificationSource: String,
    val stepFree: StepFreeStatus = StepFreeStatus.UNKNOWN
) {
    init { require(verificationSource.isNotBlank()) }
}

data class PedestrianNetwork(
    val nodes: List<WalkwayNode>,
    val edges: List<WalkwayEdge>,
    val entrances: List<VerifiedEntrance>,
    val sourceAttribution: String,
    /** Audited polygon for the area covered by this graph. */
    val coverageBoundary: List<GeoPoint>
) {
    init {
        require(sourceAttribution.isNotBlank())
        require(nodes.map { it.id }.distinct().size == nodes.size)
        require(edges.map { it.id }.distinct().size == edges.size)
        require(coverageBoundary.size >= 3)
        val nodeIds = nodes.mapTo(mutableSetOf()) { it.id }
        require(edges.all { it.from in nodeIds && it.to in nodeIds && it.from != it.to })
        require(entrances.all { it.nodeId in nodeIds })
    }

    val isUsable: Boolean get() = nodes.isNotEmpty() && edges.isNotEmpty() && entrances.isNotEmpty()

    fun covers(point: GeoPoint): Boolean {
        var inside = false
        var previous = coverageBoundary.last()
        for (current in coverageBoundary) {
            val crosses = (current.latitude > point.latitude) != (previous.latitude > point.latitude) &&
                point.longitude < (previous.longitude - current.longitude) *
                (point.latitude - current.latitude) / (previous.latitude - current.latitude) + current.longitude
            if (crosses) inside = !inside
            previous = current
        }
        return inside
    }
}

sealed interface PedestrianRouteResult {
    data class Available(val points: List<GeoPoint>, val distanceMeters: Double, val accessibilityVerified: Boolean) : PedestrianRouteResult
    data class Unavailable(val reason: Reason) : PedestrianRouteResult

    enum class Reason { NETWORK_NOT_VERIFIED, LOCATION_NOT_USABLE, OUTSIDE_COVERAGE, NO_NEARBY_WALKWAY, NO_CONNECTED_ROUTE, ENTRANCE_NOT_VERIFIED, ACCESSIBILITY_NOT_VERIFIED }
}

object PedestrianRoutePlanner {
    const val maxSnapDistanceMeters = 20.0

    /** Distance to the closest mapped line segment, for auditing endpoints against the graph. */
    fun nearestMappedWalkwayDistance(point: GeoPoint, network: PedestrianNetwork): Double? =
        nearestEdge(point, network.edges, network.nodes.associateBy { it.id })?.distance

    fun route(
        reading: LocationReading,
        nowMillis: Long,
        destinationBuildingId: String,
        network: PedestrianNetwork?,
        accessibilityRequested: Boolean = false
    ): PedestrianRouteResult {
        if (LocationQualityPolicy.evaluate(reading, nowMillis) != LocationQuality.Usable) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.LOCATION_NOT_USABLE)
        }
        if (network == null || !network.isUsable) {
            return PedestrianRouteResult.Unavailable(if (accessibilityRequested) PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED else PedestrianRouteResult.Reason.NETWORK_NOT_VERIFIED)
        }
        val destinationEntrances = network.entrances.filter { it.buildingId == destinationBuildingId }
        if (destinationEntrances.isEmpty()) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ENTRANCE_NOT_VERIFIED)
        }
        val usableEntrances = destinationEntrances.filter {
            !accessibilityRequested || it.stepFree == StepFreeStatus.VERIFIED_STEP_FREE
        }
        if (usableEntrances.isEmpty()) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED)
        }

        val start = GeoPoint(reading.latitude, reading.longitude)
        if (!network.covers(start)) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.OUTSIDE_COVERAGE)
        }
        val snap = nearestEdge(start, network.edges.filter {
            !accessibilityRequested || it.stepFree == StepFreeStatus.VERIFIED_STEP_FREE
        }, network.nodes.associateBy { it.id })
            ?: return PedestrianRouteResult.Unavailable(if (accessibilityRequested) PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED else PedestrianRouteResult.Reason.NO_NEARBY_WALKWAY)
        if (snap.distance > maxSnapDistanceMeters) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.NO_NEARBY_WALKWAY)
        }

        val nodes = network.nodes.associateBy { it.id }
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        network.edges.filter { !accessibilityRequested || it.stepFree == StepFreeStatus.VERIFIED_STEP_FREE }.forEach { edge ->
            val a = nodes.getValue(edge.from).point
            val b = nodes.getValue(edge.to).point
            val length = meters(a, b)
            adjacency.getOrPut(edge.from) { mutableListOf() }.add(edge.to to length)
            adjacency.getOrPut(edge.to) { mutableListOf() }.add(edge.from to length)
        }

        // Model the snapped position as a temporary graph node that splits its mapped edge.
        val startId = "__route_start__"
        val edge = snap.edge
        val a = nodes.getValue(edge.from).point
        val b = nodes.getValue(edge.to).point
        adjacency.getOrPut(startId) { mutableListOf() }.apply {
            add(edge.from to meters(snap.point, a))
            add(edge.to to meters(snap.point, b))
        }
        adjacency.getOrPut(edge.from) { mutableListOf() }.add(startId to meters(snap.point, a))
        adjacency.getOrPut(edge.to) { mutableListOf() }.add(startId to meters(snap.point, b))

        val targets = usableEntrances.mapTo(mutableSetOf()) { it.nodeId }
        val distances = mutableMapOf(startId to 0.0)
        val previous = mutableMapOf<String, String>()
        val unvisited = mutableSetOf(startId)
        var target: String? = null
        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { distances[it] ?: Double.POSITIVE_INFINITY } ?: break
            unvisited.remove(current)
            if (current in targets) {
                target = current
                break
            }
            for ((next, cost) in adjacency[current].orEmpty()) {
                val candidate = (distances[current] ?: Double.POSITIVE_INFINITY) + cost
                if (candidate < (distances[next] ?: Double.POSITIVE_INFINITY)) {
                    distances[next] = candidate
                    previous[next] = current
                    unvisited += next
                }
            }
        }
        val chosenTarget = target ?: return PedestrianRouteResult.Unavailable(
            if (accessibilityRequested && network.edges.any { it.stepFree == StepFreeStatus.UNKNOWN }) PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED
            else PedestrianRouteResult.Reason.NO_CONNECTED_ROUTE
        )
        val distance = distances.getValue(chosenTarget)
        val pathIds = mutableListOf(chosenTarget)
        while (pathIds.last() != startId) pathIds += previous[pathIds.last()] ?: return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.NO_CONNECTED_ROUTE)
        val points = pathIds.asReversed().mapNotNull { id -> if (id == startId) snap.point else nodes[id]?.point }
        return PedestrianRouteResult.Available(points, distance, accessibilityRequested)
    }

    /** Manual building starts are usable only when that building has a surveyed entrance node. */
    fun routeFromBuilding(
        startBuildingId: String,
        destinationBuildingId: String,
        network: PedestrianNetwork?,
        accessibilityRequested: Boolean = false
    ): PedestrianRouteResult {
        if (network == null || !network.isUsable) {
            return PedestrianRouteResult.Unavailable(if (accessibilityRequested) PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED else PedestrianRouteResult.Reason.NETWORK_NOT_VERIFIED)
        }
        val starts = network.entrances.filter { it.buildingId == startBuildingId }
        val destinations = network.entrances.filter { it.buildingId == destinationBuildingId }
        if (starts.isEmpty() || destinations.isEmpty()) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ENTRANCE_NOT_VERIFIED)
        }
        val usableStarts = starts.filter { !accessibilityRequested || it.stepFree == StepFreeStatus.VERIFIED_STEP_FREE }
        val usableDestinations = destinations.filter { !accessibilityRequested || it.stepFree == StepFreeStatus.VERIFIED_STEP_FREE }
        if (usableStarts.isEmpty() || usableDestinations.isEmpty()) {
            return PedestrianRouteResult.Unavailable(PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED)
        }

        val nodes = network.nodes.associateBy { it.id }
        val usableEdges = network.edges.filter {
            !accessibilityRequested || it.stepFree == StepFreeStatus.VERIFIED_STEP_FREE
        }
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        usableEdges.forEach { edge ->
            val a = nodes.getValue(edge.from).point
            val b = nodes.getValue(edge.to).point
            val length = meters(a, b)
            adjacency.getOrPut(edge.from) { mutableListOf() }.add(edge.to to length)
            adjacency.getOrPut(edge.to) { mutableListOf() }.add(edge.from to length)
        }
        val path = usableStarts.flatMap { start ->
            usableDestinations.mapNotNull { destination ->
                shortestPath(start.nodeId, destination.nodeId, adjacency)
            }
        }.minByOrNull { it.second }
            ?: return PedestrianRouteResult.Unavailable(
                if (accessibilityRequested && network.edges.any { it.stepFree == StepFreeStatus.UNKNOWN })
                    PedestrianRouteResult.Reason.ACCESSIBILITY_NOT_VERIFIED
                else PedestrianRouteResult.Reason.NO_CONNECTED_ROUTE
            )
        val points = path.first.mapNotNull { nodes[it]?.point }
        return PedestrianRouteResult.Available(points, path.second, accessibilityRequested)
    }

    private fun shortestPath(
        startId: String,
        targetId: String,
        adjacency: Map<String, List<Pair<String, Double>>>
    ): Pair<List<String>, Double>? {
        val distances = mutableMapOf(startId to 0.0)
        val previous = mutableMapOf<String, String>()
        val unvisited = mutableSetOf(startId)
        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { distances[it] ?: Double.POSITIVE_INFINITY } ?: break
            unvisited.remove(current)
            if (current == targetId) break
            for ((next, cost) in adjacency[current].orEmpty()) {
                val candidate = (distances[current] ?: Double.POSITIVE_INFINITY) + cost
                if (candidate < (distances[next] ?: Double.POSITIVE_INFINITY)) {
                    distances[next] = candidate
                    previous[next] = current
                    unvisited += next
                }
            }
        }
        val distance = distances[targetId] ?: return null
        val ids = mutableListOf(targetId)
        while (ids.last() != startId) ids += previous[ids.last()] ?: return null
        return ids.asReversed() to distance
    }

    private data class Snap(val edge: WalkwayEdge, val point: GeoPoint, val distance: Double)

    private fun nearestEdge(start: GeoPoint, edges: List<WalkwayEdge>, nodes: Map<String, WalkwayNode>): Snap? =
        edges.mapNotNull { edge ->
            val a = nodes[edge.from]?.point ?: return@mapNotNull null
            val b = nodes[edge.to]?.point ?: return@mapNotNull null
            val latScale = 111_320.0
            val lonScale = latScale * kotlin.math.cos(radians(start.latitude))
            val ax = (a.longitude - start.longitude) * lonScale
            val ay = (a.latitude - start.latitude) * latScale
            val bx = (b.longitude - start.longitude) * lonScale
            val by = (b.latitude - start.latitude) * latScale
            val dx = bx - ax
            val dy = by - ay
            val denominator = dx * dx + dy * dy
            val t = if (denominator == 0.0) 0.0 else (-(ax * dx + ay * dy) / denominator).coerceIn(0.0, 1.0)
            val point = GeoPoint(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)
            val distance = meters(start, point)
            Snap(edge, point, distance)
        }.minByOrNull { it.distance }

    private fun meters(a: GeoPoint, b: GeoPoint): Double {
        val lat = radians((a.latitude + b.latitude) / 2.0)
        val north = (b.latitude - a.latitude) * 111_320.0
        val east = (b.longitude - a.longitude) * 111_320.0 * kotlin.math.cos(lat)
        return kotlin.math.sqrt(north * north + east * east)
    }

    private fun radians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0
}
