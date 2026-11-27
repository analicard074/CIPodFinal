package com.example.cipod_final.indoor

data class IndoorNode(
    val id: String,
    val floorPlanId: String,
    val position: PlanPoint,
    val label: String,
    val roomId: String? = null,
)

enum class IndoorConnection { Walk, Elevator, Stairs }

// Cost is relative image-space distance, not a surveyed distance or walking time.
data class IndoorEdge(
    val from: String,
    val to: String,
    val cost: Double,
    val connection: IndoorConnection = IndoorConnection.Walk,
) {
    init {
        require(cost.isFinite() && cost > 0 && from != to)
    }
}

data class IndoorEntrance(val nodeId: String, val label: String, val isMain: Boolean = false)

data class IndoorRoute(val nodes: List<IndoorNode>, val edges: List<IndoorEdge>) {
    fun segments(planId: String): List<Pair<PlanPoint, PlanPoint>> =
        nodes.zipWithNext().filter { (a, b) -> a.floorPlanId == planId && b.floorPlanId == planId }
            .map { (a, b) -> a.position to b.position }

    val floorPlanIds: List<String> get() = nodes.map { it.floorPlanId }.distinct()
}

class IndoorRoutingGraph(
    val buildingId: String,
    val planRevisions: Map<String, String>,
    val nodes: List<IndoorNode>,
    val edges: List<IndoorEdge>,
    val entrances: List<IndoorEntrance>,
) {
    private val byId = nodes.associateBy { it.id }

    // Each connection can be walked in either direction.
    private val edgesByNode = mutableMapOf<String, MutableList<IndoorEdge>>()
    val mainEntrance: IndoorEntrance? = entrances.singleOrNull { it.isMain }

    init {
        for (edge in edges) {
            edgesByNode.getOrPut(edge.from) { mutableListOf() }.add(edge)
            edgesByNode.getOrPut(edge.to) { mutableListOf() }.add(edge)
        }
        require(byId.size == nodes.size)
        require(nodes.all { it.floorPlanId in planRevisions })
        require(edges.all { it.from in byId && it.to in byId })
        require(edges.all {
            it.connection != IndoorConnection.Walk || byId.getValue(it.from).floorPlanId == byId.getValue(
                it.to
            ).floorPlanId
        })
        require(entrances.all { it.nodeId in byId })
        require(entrances.count { it.isMain } <= 1)
    }

    fun supports(room: DirectoryRoom): Boolean =
        room.buildingId == buildingId && nodes.any { it.roomId == room.id }

    fun matches(plans: List<FloorPlan>): Boolean = planRevisions.all { (id, revision) ->
        plans.any { it.id == id && it.buildingId == buildingId && it.revision == revision }
    }

    // Outdoor navigation always enters through the main entrance, even if another is closer.
    fun routeFromOutside(
        roomId: String,
        avoidStairs: Boolean = true,
        avoidElevators: Boolean = false
    ): IndoorRoute? {
        val entrance = mainEntrance ?: return null
        return route(entrance.nodeId, roomId, avoidStairs, avoidElevators)
    }

    // Compare all exits from the starting room and all entrances to the destination.
    fun routeFromRoom(
        startRoomId: String, roomId: String, avoidStairs: Boolean = true,
        avoidElevators: Boolean = false
    ): IndoorRoute? =
        nodes.filter { node -> node.roomId == startRoomId }
            .mapNotNull { door -> route(door.id, roomId, avoidStairs, avoidElevators) }
            .minByOrNull { path -> path.edges.sumOf { edge -> edge.cost } }

    // Dijkstra's algorithm: visit the cheapest remaining node until a room doorway is reached.
    fun route(
        startId: String,
        roomId: String,
        avoidStairs: Boolean = true,
        avoidElevators: Boolean = false
    ): IndoorRoute? {
        if (startId !in byId) return null
        val targets = nodes.filter { it.roomId == roomId }.map { it.id }.toSet()
        if (targets.isEmpty()) return null
        val distances = mutableMapOf(startId to 0.0)
        val previous = mutableMapOf<String, Pair<String, IndoorEdge>>()
        val visited = mutableSetOf<String>()
        while (true) {

            // Choose the unvisited node with the lowest total cost.
            val unvisited = distances.filterKeys { it !in visited }
            val current = unvisited.minByOrNull { it.value }?.key ?: return null

            if (current in targets) {
                val path = mutableListOf(byId.getValue(current))
                val connections = mutableListOf<IndoorEdge>()
                var cursor = current
                while (cursor != startId) {
                    val (parent, edge) = previous.getValue(cursor)
                    connections += edge
                    path += byId.getValue(parent)
                    cursor = parent
                }
                return IndoorRoute(path.reversed(), connections.reversed())
            }
            visited += current
            for (edge in edgesByNode[current].orEmpty()) {
                if (avoidStairs && edge.connection == IndoorConnection.Stairs) continue
                if (avoidElevators && edge.connection == IndoorConnection.Elevator) continue

                val next = if (edge.from == current) edge.to else edge.from

                // Calculate the cost of reaching this neighbor through the current node.
                val candidate = distances.getValue(current) + edge.cost
                val bestDistance = distances[next] ?: Double.POSITIVE_INFINITY

                // Save the cheaper route and remember where it came from.
                if (next !in visited && candidate < bestDistance) {
                    distances[next] = candidate
                    previous[next] = current to edge
                }
            }
        }
    }
}
