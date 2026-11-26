package com.example.cipod_final.indoor

import kotlin.math.hypot

/**
 * Draft tracing of the bundled Broome pictures.
 * Coordinates are measured pixels converted to PlanPoint; gaps, not room centers, are targets.
 */
object BroomeIndoorRoutes {
    private const val FIRST = "broome-library-first-floor-draft"
    private const val SECOND = "broome-library-second-floor-draft"

    val graph: IndoorRoutingGraph by lazy { buildGraph() }

    fun current(): IndoorRoutingGraph? = graph.takeIf { it.matches(FloorPlanCatalog.plans) }

    private fun buildGraph(): IndoorRoutingGraph {
        val nodes = mutableListOf<IndoorNode>()
        val edges = mutableListOf<IndoorEdge>()
        fun point(id: String, floor: String, x: Int, y: Int, label: String, room: String? = null) {
            val width = if (floor == FIRST) 2414f else 1224f
            val height = if (floor == FIRST) 2149f else 1105f
            val roomId = if (room == null) null else "broome-library-$room"
            nodes += IndoorNode(id, floor, PlanPoint(x / width, y / height), label, roomId)
        }

        fun link(a: String, b: String, kind: IndoorConnection = IndoorConnection.Walk) {
            val from = nodes.first { it.id == a }
            val to = nodes.first { it.id == b }
            val cost = if (kind == IndoorConnection.Walk) {
                val aspect = if (from.floorPlanId == FIRST) 2414.0 / 2149 else 1224.0 / 1105
                hypot(
                    (from.position.x - to.position.x) * aspect,
                    (from.position.y - to.position.y).toDouble()
                )
                    .coerceAtLeast(.0001)
            } else .15
            edges += IndoorEdge(a, b, cost, kind)
        }

        fun chain(vararg ids: String) {
            for (index in 0 until ids.lastIndex) link(ids[index], ids[index + 1])
        }

        fun door(id: String, floor: String, x: Int, y: Int, room: String, approach: String) {
            point(id, floor, x, y, "Room $room doorway", room)
            link(approach, id)
        }

        // First floor
        val firstPoints = listOf(
            Triple("top-center-entry", 1285, 75), Triple("f1-nw", 585, 385),
            Triple("f1-nw-door", 330, 385), Triple("f1-north", 1285, 385),
            Triple("f1-ne", 1988, 385), Triple("f1-ne-door", 2237, 385),
            Triple("f1-west-mid", 585, 680), Triple("f1-west-1360", 585, 742),
            Triple("f1-west-1330", 585, 1015), Triple("f1-west-low", 585, 1090),
            Triple("f1-west-bottom", 585, 1460), Triple("f1-west-south", 585, 1850),
            Triple("f1-1310-approach", 446, 1850),
            Triple("f1-mid", 1285, 680), Triple("f1-center", 1285, 1090),
            Triple("f1-east-mid", 1988, 680), Triple("f1-east-1760", 1988, 635),
            Triple("f1-east-1756-n", 1988, 662),
            Triple("f1-east-1756", 1988, 855), Triple("f1-east-low", 1988, 1090),
            Triple("f1-1750-approach", 2070, 1090),
            Triple("f1-east-bottom", 1988, 1460), Triple("f1-east-south", 1988, 1850),
            Triple("f1-1710-approach", 2120, 1850),
            Triple("f1-checkout-approach", 918, 1090),
            Triple("f1-lift-hall", 1285, 1230), Triple("f1-lift-approach", 1396, 1230),
            Triple("f1-lift", 1396, 1195), Triple("f1-checkout-east", 1195, 1230),
            Triple("f1-pool-approach", 1253, 1230), Triple("reflection-pool-entry", 1253, 1425),
            Triple("f1-courtyard", 1253, 1460), Triple("f1-cafe-approach", 1548, 1460),
            Triple("f1-north-lift-hall", 1285, 320), Triple("f1-north-lift", 1315, 320),
        )
        firstPoints.forEach { (id, x, y) ->
            point(
                id, FIRST, x, y, when (id) {
                    "top-center-entry" -> "Back Entrance"
                    "reflection-pool-entry" -> "Main Entrance"
                    "f1-lift" -> "Main entrance elevator, first floor"
                    "f1-north-lift" -> "North elevator, first floor"
                    "f1-courtyard" -> "Courtyard passage"
                    else -> "First-floor hallway"
                }
            )
        }
        chain("f1-nw-door", "f1-nw", "f1-north", "f1-ne", "f1-ne-door")
        chain(
            "f1-nw",
            "f1-west-mid",
            "f1-west-1360",
            "f1-west-1330",
            "f1-west-low",
            "f1-west-bottom",
            "f1-west-south"
        )
        chain(
            "f1-north", "f1-mid", "f1-center", "f1-lift-hall", "f1-pool-approach",
            "reflection-pool-entry", "f1-courtyard", "f1-cafe-approach"
        )
        chain("f1-west-mid", "f1-mid", "f1-east-mid")
        chain("f1-west-low", "f1-checkout-approach", "f1-center", "f1-east-low")
        chain(
            "f1-ne",
            "f1-east-1760",
            "f1-east-1756-n",
            "f1-east-mid",
            "f1-east-1756",
            "f1-east-low",
            "f1-east-bottom",
            "f1-east-south"
        )
        chain("f1-lift-hall", "f1-lift-approach", "f1-lift")
        link("f1-lift-hall", "f1-checkout-east")
        chain("f1-north", "f1-north-lift-hall", "f1-north-lift")
        link("f1-west-south", "f1-1310-approach")
        link("f1-east-south", "f1-1710-approach")
        link("f1-east-low", "f1-1750-approach")
        door("d1310", FIRST, 446, 1878, "1310", "f1-1310-approach")
        door("d1360-n", FIRST, 330, 416, "1360", "f1-nw-door")
        door("d1360-e", FIRST, 558, 742, "1360", "f1-west-1360")
        door("d1330-n", FIRST, 558, 1015, "1330", "f1-west-1330")
        // The second 1330 opening is at the lower right corner
        point("f1-1330-s-approach", FIRST, 585, 1425, "Computer lab approach")
        link("f1-west-low", "f1-1330-s-approach")
        link("f1-1330-s-approach", "f1-west-bottom")
        door("d1330-s", FIRST, 558, 1425, "1330", "f1-1330-s-approach")
        door("d1710", FIRST, 2120, 1878, "1710", "f1-1710-approach")
        door("d1750", FIRST, 2098, 1080, "1750", "f1-1750-approach")
        door("d1756", FIRST, 2010, 855, "1756", "f1-east-1756")
        door("d1756-n", FIRST, 2010, 662, "1756", "f1-east-1756-n")
        door("d1760-w", FIRST, 2010, 635, "1760", "f1-east-1760")
        door("d1760-n", FIRST, 2237, 416, "1760", "f1-ne-door")
        door("d1531-n", FIRST, 918, 1135, "1531", "f1-checkout-approach")
        door("d1531-e", FIRST, 1171, 1210, "1531", "f1-checkout-east")
        door("d1537", FIRST, 1548, 1425, "1537", "f1-cafe-approach")
        door("d1550", FIRST, 1250, 660, "1550", "f1-mid")
        door("d1720", FIRST, 2015, 1502, "1720", "f1-east-bottom")

        // Second floor
        val secondPoints = listOf(
            Triple("f2-nw", 299, 216), Triple("f2-west-mid", 299, 370),
            Triple("f2-2330-n-approach", 299, 468), Triple("f2-2325-n-approach", 299, 590),
            Triple("f2-west-low", 299, 580), Triple("f2-west-south", 299, 732),
            Triple("f2-north", 661, 216), Triple("f2-ne", 1018, 216),
            Triple("f2-east-mid", 1018, 370), Triple("f2-east-low", 1018, 580),
            Triple("f2-east-south", 1018, 850),
            Triple("f2-inner-west-mid", 450, 370), Triple("f2-inner-east-mid", 873, 370),
            Triple("f2-inner-west-low", 450, 580), Triple("f2-inner-east-low", 873, 580),
            Triple("f2-center-low", 661, 580), Triple("f2-lift-hall", 661, 654),
            Triple("f2-lift-approach", 718, 654), Triple("f2-lift", 718, 635),
            Triple("f2-north-lift-hall", 661, 183), Triple("f2-north-lift", 678, 183),
            Triple("f2-north-center-hall", 661, 123),
            Triple("f2-2480-w", 317, 216), Triple("f2-2480-e", 465, 216),
            Triple("f2-2490-w", 513, 216), Triple("f2-2490-e", 638, 216),
            Triple("f2-2680-approach", 833, 216),
        )
        secondPoints.forEach { (id, x, y) ->
            point(
                id, SECOND, x, y, when (id) {
                    "f2-lift" -> "Main entrance elevator, second floor"
                    "f2-north-lift" -> "North elevator, second floor"
                    else -> "Second-floor hallway"
                }
            )
        }
        chain(
            "f2-nw",
            "f2-2480-w",
            "f2-2480-e",
            "f2-2490-w",
            "f2-2490-e",
            "f2-north",
            "f2-2680-approach",
            "f2-ne"
        )
        chain(
            "f2-nw", "f2-west-mid", "f2-2330-n-approach", "f2-west-low",
            "f2-2325-n-approach", "f2-west-south"
        )
        chain("f2-ne", "f2-east-mid", "f2-east-low", "f2-east-south")
        chain("f2-west-mid", "f2-inner-west-mid", "f2-inner-east-mid", "f2-east-mid")
        chain(
            "f2-west-low",
            "f2-inner-west-low",
            "f2-center-low",
            "f2-inner-east-low",
            "f2-east-low"
        )
        link("f2-inner-west-mid", "f2-inner-west-low")
        link("f2-inner-east-mid", "f2-inner-east-low")
        point("f2-center-north", SECOND, 661, 370, "North atrium hallway")
        link("f2-north", "f2-center-north")
        chain("f2-center-low", "f2-lift-hall", "f2-lift-approach", "f2-lift")
        chain("f2-north", "f2-north-lift-hall", "f2-north-center-hall")
        link("f2-north-lift-hall", "f2-north-lift")
        link("f1-lift", "f2-lift", IndoorConnection.Elevator)
        link("f1-north-lift", "f2-north-lift", IndoorConnection.Elevator)
        // Walk from the back entrance hallway into the stairwell before changing floors
        point("f1-north-stairs-approach", FIRST, 1285, 120, "Back entrance stair approach")
        point("f2-north-stairs-approach", SECOND, 661, 80, "Back entrance stair approach")
        point("f1-north-stairs", FIRST, 1396, 120, "Back Entrance stairs, first floor")
        point("f2-north-stairs", SECOND, 718, 80, "Back Entrance stairs, second floor")
        chain("top-center-entry", "f1-north-stairs-approach", "f1-north-lift-hall")
        link("f1-north-stairs-approach", "f1-north-stairs")
        chain("f2-north-center-hall", "f2-north-stairs-approach", "f2-north-stairs")
        link("f1-north-stairs", "f2-north-stairs", IndoorConnection.Stairs)
        // Main entrance stairs beside the cafe/gallery, reached from the elevator lobby
        point("f1-main-stairs", FIRST, 1396, 1320, "Main entrance stairs, first floor")
        point("f2-main-stairs", SECOND, 718, 710, "Main entrance stairs, second floor")
        link("f1-lift-approach", "f1-main-stairs")
        link("f2-lift-approach", "f2-main-stairs")
        link("f1-main-stairs", "f2-main-stairs", IndoorConnection.Stairs)
        door("d2325", SECOND, 287, 732, "2325", "f2-west-south")
        door("d2325-n", SECOND, 287, 590, "2325", "f2-2325-n-approach")
        door("d2330", SECOND, 287, 575, "2330", "f2-west-low")
        door("d2330-n", SECOND, 287, 468, "2330", "f2-2330-n-approach")
        door("d2360", SECOND, 299, 367, "2360", "f2-west-mid")
        door("d2480-w", SECOND, 317, 200, "2480", "f2-2480-w")
        door("d2480-e", SECOND, 465, 200, "2480", "f2-2480-e")
        door("d2490-w", SECOND, 513, 200, "2490", "f2-2490-w")
        door("d2490-e", SECOND, 638, 200, "2490", "f2-2490-e")
        door("d2675", SECOND, 678, 123, "2675", "f2-north-center-hall")
        door("d2680", SECOND, 833, 200, "2680", "f2-2680-approach")
        point("f2-2680-w-approach", SECOND, 800, 123, "Writing center entrance passage")
        link("f2-north-center-hall", "f2-2680-w-approach")
        door("d2680-w", SECOND, 826, 120, "2680", "f2-2680-w-approach")
        door("d2720", SECOND, 1032, 850, "2720", "f2-east-south")
        door("d2730", SECOND, 1032, 580, "2730", "f2-east-low")
        return IndoorRoutingGraph(
            "broome-library", mapOf(FIRST to "reference-draft-1", SECOND to "reference-draft-1"),
            nodes, edges, listOf(
                IndoorEntrance("reflection-pool-entry", "Main Entrance", isMain = true),
                IndoorEntrance("top-center-entry", "Back Entrance"),
            )
        )
    }
}
