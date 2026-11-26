package com.example.cipod_final

import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.indoor.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IndoorRoutingTest {
    private fun node(id: String, floor: String = "one", room: String? = null) =
        IndoorNode(id, floor, PlanPoint(.5f, .5f), id, room)

    @Test fun outsideRoutesAlwaysStartAtMainEntranceWithoutFallingBackToBackEntrance() {
        val graph = IndoorRoutingGraph("building", mapOf("one" to "v1"),
            listOf(node("main"), node("back"), node("door", room = "room"),
                node("back-only", room = "back-only-room")),
            listOf(IndoorEdge("main", "door", 10.0), IndoorEdge("back", "door", 1.0)),
            listOf(IndoorEntrance("back", "Back Entrance"), IndoorEntrance("main", "Main Entrance", isMain = true)))
        assertEquals(listOf("main", "door"), graph.routeFromOutside("room")?.nodes?.map { it.id })
        val disconnected = IndoorRoutingGraph("building", graph.planRevisions, graph.nodes,
            listOf(IndoorEdge("back", "back-only", 1.0)), graph.entrances)
        assertNotNull(disconnected.route("back", "back-only-room"))
        assertNull(disconnected.routeFromOutside("back-only-room"))
        val noMain = IndoorRoutingGraph("building", graph.planRevisions, graph.nodes, graph.edges,
            listOf(IndoorEntrance("back", "Back Entrance")))
        assertNull(noMain.routeFromOutside("room"))
    }

    @Test fun everySupportedBroomeRoomRoutesFromOutsideThroughMainEntrance() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        assertEquals("reflection-pool-entry", graph.mainEntrance?.nodeId)
        RoomDirectory.search("broome-library").filter(graph::supports).forEach { room ->
            val route = assertNotNull(graph.routeFromOutside(room.id))
            assertEquals("reflection-pool-entry", route.nodes.first().id)
            assertEquals(room.id, route.nodes.last().roomId)
        }
    }

    @Test fun stairsAndElevatorSelectionsProduceSeparateRoutesForEveryUpstairsRoom() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        RoomDirectory.search("broome-library").filter(graph::supports).forEach { room ->
            val stairs = assertNotNull(graph.routeFromOutside(room.id, avoidStairs = false, avoidElevators = true))
            val elevator = assertNotNull(graph.routeFromOutside(room.id, avoidStairs = true, avoidElevators = false))
            assertEquals("reflection-pool-entry", stairs.nodes.first().id)
            assertEquals("reflection-pool-entry", elevator.nodes.first().id)
            assertFalse(stairs.edges.any { it.connection == IndoorConnection.Elevator })
            assertFalse(elevator.edges.any { it.connection == IndoorConnection.Stairs })
            if (room.number.startsWith("2")) {
                assertTrue(stairs.edges.any { it.connection == IndoorConnection.Stairs })
                assertTrue(elevator.edges.any { it.connection == IndoorConnection.Elevator })
                assertNotEquals(stairs.nodes.map { it.id }, elevator.nodes.map { it.id })
            }
        }
    }

    @Test fun stairsSelectionDoesNotFallBackToElevatorWhenNoStairsAreConnected() {
        val graph = IndoorRoutingGraph("building", mapOf("one" to "v1", "two" to "v1"),
            listOf(node("start"), node("door", "two", "room")),
            listOf(IndoorEdge("start", "door", 1.0, IndoorConnection.Elevator)),
            listOf(IndoorEntrance("start", "Main Entrance", isMain = true)))
        assertNull(graph.routeFromOutside("room", avoidStairs = false, avoidElevators = true))
        assertNotNull(graph.routeFromOutside("room", avoidStairs = true))
    }

    @Test fun bothSetsOfStairsAndElevatorsAreUsableAndChosenByTotalRouteCost() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        assertEquals(2, graph.edges.count { it.connection == IndoorConnection.Stairs })
        assertEquals(2, graph.edges.count { it.connection == IndoorConnection.Elevator })
        listOf(false, true).forEach { elevator ->
            val kind = if (elevator) IndoorConnection.Elevator else IndoorConnection.Stairs
            val main = assertNotNull(graph.routeFromOutside("broome-library-2330",
                avoidStairs = elevator, avoidElevators = !elevator))
            val mainConnector = main.edges.single { it.connection == kind }
            assertEquals(if (elevator) "f1-lift" else "f1-main-stairs", mainConnector.from)
            val north = assertNotNull(graph.route("top-center-entry", "broome-library-2675",
                avoidStairs = elevator, avoidElevators = !elevator))
            val northConnector = north.edges.single { it.connection == kind }
            assertEquals(if (elevator) "f1-north-lift" else "f1-north-stairs", northConnector.from)
        }
    }

    @Test fun choosesLowestTotalCostDoorRatherThanFewestEdgesOrFirstDoor() {
        val graph = IndoorRoutingGraph("building", mapOf("one" to "v1"),
            listOf(node("start"), node("junction"), node("near", room = "room"), node("far", room = "room")),
            listOf(IndoorEdge("start", "far", 8.0), IndoorEdge("start", "junction", 1.0),
                IndoorEdge("junction", "near", 2.0)), listOf(IndoorEntrance("start", "Entry")))
        assertEquals(listOf("start", "junction", "near"), graph.route("start", "room")?.nodes?.map { it.id })
        assertEquals("near", graph.route("junction", "room")?.nodes?.last()?.id)
        assertNull(graph.route("missing", "room"))
        assertNull(graph.route("start", "unknown-room"))
    }

    @Test fun stairsCanBeExcludedAndDisconnectedRoomsReturnNoRoute() {
        val graph = IndoorRoutingGraph("building", mapOf("one" to "v1", "two" to "v1"),
            listOf(node("start"), node("stairs", "two"), node("lift", "two"),
                node("door", "two", "room"), node("isolated", "two", "isolated-room")),
            listOf(IndoorEdge("start", "stairs", 1.0, IndoorConnection.Stairs),
                IndoorEdge("stairs", "door", 1.0),
                IndoorEdge("start", "lift", 5.0, IndoorConnection.Elevator),
                IndoorEdge("lift", "door", 1.0)), listOf(IndoorEntrance("start", "Entry")))
        val elevatorRoute = assertNotNull(graph.route("start", "room"))
        assertTrue(elevatorRoute.edges.any { it.connection == IndoorConnection.Elevator })
        assertFalse(elevatorRoute.edges.any { it.connection == IndoorConnection.Stairs })
        assertEquals("stairs", graph.route("start", "room", avoidStairs = false)?.nodes?.get(1)?.id)
        assertNull(graph.route("start", "isolated-room"))
        assertTrue(elevatorRoute.segments("one").isEmpty())
        assertEquals(1, elevatorRoute.segments("two").size)
    }

    @Test fun everySupportedBroomeRoomIsReachableFromEveryEntry() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        val supported = RoomDirectory.search("broome-library").filter(graph::supports)
        assertEquals(20, supported.size)
        graph.entrances.forEach { entry ->
            supported.forEach { room ->
                val route = assertNotNull(graph.route(entry.nodeId, room.id), "${entry.label} to ${room.number}")
                assertEquals(entry.nodeId, route.nodes.first().id)
                assertEquals(room.id, route.nodes.last().roomId)
                assertEquals(room.floorPlanId, route.nodes.last().floorPlanId)
                if (room.number.startsWith("2")) {
                    assertTrue(route.edges.any { it.connection == IndoorConnection.Elevator })
                }
            }
        }
    }

    @Test fun confirmedEntrancesUseThePoolDoorwayAndTopCenterMark() {
        val graph = BroomeIndoorRoutes.graph
        assertEquals(setOf("reflection-pool-entry", "top-center-entry"), graph.entrances.map { it.nodeId }.toSet())
        val pool = graph.nodes.single { it.id == "reflection-pool-entry" }
        val top = graph.nodes.single { it.id == "top-center-entry" }
        assertEquals(PlanPoint(1253f / 2414, 1425f / 2149), pool.position)
        assertEquals(PlanPoint(1285f / 2414, 75f / 2149), top.position)
        assertEquals("broome-library-first-floor-draft", pool.floorPlanId)
        assertEquals(pool.floorPlanId, top.floorPlanId)
        val poolRoute = assertNotNull(graph.route(pool.id, "broome-library-2330"))
        val topRoute = assertNotNull(graph.route(top.id, "broome-library-2330"))
        assertEquals("f1-pool-approach", poolRoute.nodes[1].id)
        assertEquals("f1-north-stairs-approach", topRoute.nodes[1].id)
        assertEquals("f1-north-lift-hall", topRoute.nodes[2].id)
    }

    @Test fun backEntranceStairsDrawTheApproachIntoTheStairwellOnBothFloors() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        val upstairs = assertNotNull(graph.route("top-center-entry", "broome-library-2675",
            avoidStairs = false, avoidElevators = true))
        val transition = upstairs.edges.indexOfFirst { it.connection == IndoorConnection.Stairs }
        assertTrue(transition > 0)
        assertEquals(listOf("f1-north-stairs-approach", "f1-north-stairs", "f2-north-stairs", "f2-north-stairs-approach"),
            upstairs.nodes.subList(transition - 1, transition + 3).map { it.id })
        listOf("f1" to "broome-library-first-floor-draft", "f2" to "broome-library-second-floor-draft").forEach { (prefix, planId) ->
            val approach = graph.nodes.single { it.id == "$prefix-north-stairs-approach" }
            val stairs = graph.nodes.single { it.id == "$prefix-north-stairs" }
            assertTrue(stairs.position.x > approach.position.x)
            assertTrue(upstairs.segments(planId).any {
                it == (approach.position to stairs.position) || it == (stairs.position to approach.position)
            })
        }
        val downstairs = assertNotNull(graph.routeFromRoom("broome-library-2675", "broome-library-1756",
            avoidStairs = false, avoidElevators = true))
        assertTrue(downstairs.nodes.any { it.id == "f2-north-stairs" })
        assertTrue(downstairs.nodes.any { it.id == "f1-north-stairs" })
    }

    @Test fun alternateDoorsAreRetainedAndChosenBasedOnStartingPosition() {
        val graph = BroomeIndoorRoutes.graph
        assertEquals(2, graph.nodes.count { it.roomId == "broome-library-1330" })
        assertNotEquals(graph.route("f1-west-1330", "broome-library-1330")?.nodes?.last()?.id,
            graph.route("f1-west-bottom", "broome-library-1330")?.nodes?.last()?.id)
        assertEquals(2, graph.nodes.count { it.roomId == "broome-library-2480" })
        assertEquals(2, graph.nodes.count { it.roomId == "broome-library-2490" })
    }

    @Test fun everyMultiEntranceRoomUsesTheClosestDoorAlongTheSelectedRoute() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        val multiEntranceRooms = graph.nodes.filter { it.roomId != null }
            .groupBy { it.roomId!! }.filterValues { it.size > 1 }
        assertEquals(setOf("1330", "1360", "1531", "1756", "1760", "2325", "2330", "2480", "2490", "2680"),
            multiEntranceRooms.keys.map { it.substringAfterLast('-') }.toSet())
        multiEntranceRooms.forEach { (roomId, doors) ->
            listOf(false, true).forEach { elevator ->
                val route = assertNotNull(graph.routeFromOutside(roomId,
                    avoidStairs = elevator, avoidElevators = !elevator))
                // Compare against independently routing to each door as the only destination.
                val individualCosts = doors.associate { door ->
                    val singleDoor = IndoorRoutingGraph(graph.buildingId, graph.planRevisions,
                        graph.nodes.map { node ->
                            if (node.roomId == roomId && node.id != door.id) node.copy(roomId = null) else node
                        }, graph.edges, graph.entrances)
                    val candidate = assertNotNull(singleDoor.routeFromOutside(roomId,
                        avoidStairs = elevator, avoidElevators = !elevator))
                    door.id to candidate.edges.sumOf { it.cost }
                }
                assertEquals(individualCosts.values.min(), route.edges.sumOf { it.cost }, 1e-9,
                    "Closest doorway for $roomId (elevator=$elevator)")
                assertEquals(individualCosts.getValue(route.nodes.last().id), route.edges.sumOf { it.cost }, 1e-9)
                doors.forEach { door ->
                    val edge = graph.edges.single { it.from == door.id || it.to == door.id }
                    val approach = if (edge.from == door.id) edge.to else edge.from
                    val nearby = assertNotNull(graph.route(approach, roomId,
                        avoidStairs = elevator, avoidElevators = !elevator))
                    assertEquals(door.id, nearby.nodes.last().id,
                        "Use the nearby door when approaching $roomId from $approach")
                }
            }
        }
    }

    @Test fun classrooms2325And2330UseUpperOrLowerDoorDependingOnHallwayApproach() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        assertEquals(PlanPoint(287f / 1224, 468f / 1105), graph.nodes.single { it.id == "d2330-n" }.position)
        assertEquals(PlanPoint(287f / 1224, 590f / 1105), graph.nodes.single { it.id == "d2325-n" }.position)
        listOf(false, true).forEach { elevator ->
            fun selectedDoor(start: String, number: String) = assertNotNull(graph.route(start,
                "broome-library-$number", avoidStairs = elevator, avoidElevators = !elevator)).nodes.last().id
            assertEquals("d2330-n", selectedDoor("f2-west-mid", "2330"))
            assertEquals("d2330", selectedDoor("f2-west-low", "2330"))
            assertEquals("d2325-n", selectedDoor("f2-west-low", "2325"))
            assertEquals("d2325", selectedDoor("f2-west-south", "2325"))
            val fromMain = assertNotNull(graph.routeFromOutside("broome-library-2325",
                avoidStairs = elevator, avoidElevators = !elevator))
            assertEquals("d2325-n", fromMain.nodes.last().id)
        }
    }

    @Test fun remainingAlternateDoorsFollowVisibleOpeningsAndChangeWithTheApproach() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        assertEquals(PlanPoint(2010f / 2414, 662f / 2149), graph.nodes.single { it.id == "d1756-n" }.position)
        assertEquals(PlanPoint(826f / 1224, 120f / 1105), graph.nodes.single { it.id == "d2680-w" }.position)
        listOf(false, true).forEach { elevator ->
            fun door(start: String, number: String) = assertNotNull(graph.route(start,
                "broome-library-$number", avoidStairs = elevator, avoidElevators = !elevator)).nodes.last().id
            assertEquals("d1756-n", door("f1-east-mid", "1756"))
            assertEquals("d1756", door("f1-east-low", "1756"))
            assertEquals("d2680-w", door("f2-north-center-hall", "2680"))
            assertEquals("d2680", door("f2-north", "2680"))
        }
    }

    @Test fun missingLocationsAndChangedPlanRevisionsDoNotAcquireRoutes() {
        val graph = BroomeIndoorRoutes.graph
        listOf("2300", "2740", "2537").forEach { number ->
            val room = assertNotNull(RoomDirectory.find("broome-library", "broome-library-$number"))
            assertFalse(graph.supports(room))
            assertNull(graph.route("reflection-pool-entry", room.id))
        }
        assertFalse(graph.matches(FloorPlanCatalog.plans.map { it.copy(revision = "changed") }))
        assertFalse(graph.matches(emptyList()))
    }

    @Test fun roomRoutesCompareStartingExitsAndDestinationEntrancesInBothModes() {
        val graph = assertNotNull(BroomeIndoorRoutes.current())
        val supported = RoomDirectory.search("broome-library").filter(graph::supports)
        supported.forEach { start ->
            listOf(false, true).forEach { elevator ->
                val route = assertNotNull(graph.routeFromRoom(start.id, "broome-library-2330",
                    avoidStairs = elevator, avoidElevators = !elevator))
                assertEquals(start.id, route.nodes.first().roomId)
                assertEquals("broome-library-2330", route.nodes.last().roomId)
                assertFalse(route.edges.any { it.connection == if (elevator) IndoorConnection.Stairs else IndoorConnection.Elevator })
                val cheapest = graph.nodes.filter { it.roomId == start.id }.mapNotNull {
                    graph.route(it.id, "broome-library-2330", avoidStairs = elevator, avoidElevators = !elevator)
                }.minOf { it.edges.sumOf { edge -> edge.cost } }
                assertEquals(cheapest, route.edges.sumOf { it.cost }, 1e-9)
            }
        }
        assertEquals("d1330-s", graph.routeFromRoom("broome-library-1330", "broome-library-1310")?.nodes?.first()?.id)
        val sameRoom = assertNotNull(graph.routeFromRoom("broome-library-2325", "broome-library-2325"))
        assertEquals(1, sameRoom.nodes.size)
        assertTrue(sameRoom.edges.isEmpty())
        assertNull(graph.routeFromRoom("missing", "broome-library-2330"))
        assertNull(graph.routeFromRoom("broome-library-2330", "missing"))
    }

    @Test fun roomStartingPointSurvivesDestinationSelectionAndCanBeCleared() {
        val start = assertNotNull(RoomDirectory.find("broome-library", "broome-library-1330"))
        val destination = assertNotNull(RoomDirectory.find("broome-library", "broome-library-2330"))
        val state = CampusNavigationState().selectRoomStart(start).selectRoomDestination(destination)
        assertEquals(start, state.startRoom)
        assertEquals(destination, state.destinationRoom)
        assertEquals(start, state.preview("sierra-hall").startRoom)
        assertEquals(start, state.clearDestination().startRoom)
        assertNull(state.clearRoomStart().startRoom)
        assertEquals(destination, state.clearRoomStart().destinationRoom)
        assertNull(state.selectDestination("sierra-hall").startRoom)
        val unavailable = assertNotNull(RoomDirectory.find("broome-library", "broome-library-2300"))
        assertEquals(state, state.selectRoomStart(unavailable))
        assertEquals(state, state.selectRoomStart(start.copy(number = "invented")))
        val changingStart = CampusNavigationState().selectDestination("sierra-hall").selectRoomStart(start)
        assertEquals(start, changingStart.startRoom)
        assertNull(changingStart.destination)
    }

    @Test fun roomDestinationSurvivesBrowsingAndClearsWhenBuildingDestinationChanges() {
        val room = assertNotNull(RoomDirectory.find("broome-library", "broome-library-2330"))
        val state = CampusNavigationState().selectRoomDestination(room)
        assertEquals(room, state.destinationRoom)
        assertEquals("broome-library", state.destinationId)
        assertEquals(room, state.preview("sierra-hall").destinationRoom)
        assertNull(state.selectDestination("broome-library").destinationRoom)
        assertNull(state.selectDestination("sierra-hall").destinationRoomId)
        assertNull(state.clearDestination().destinationRoomId)
        val unavailable = assertNotNull(RoomDirectory.find("broome-library", "broome-library-2300"))
        assertEquals(state, state.selectRoomDestination(unavailable))
        assertEquals(state, state.selectRoomDestination(room.copy(number = "invented")))
    }
}
