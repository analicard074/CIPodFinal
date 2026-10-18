package com.example.cipod_final

import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.FloorPlanJson
import com.example.cipod_final.indoor.PlanPoint
import com.example.cipod_final.indoor.RoomMarker
import com.example.cipod_final.indoor.RoomMarkerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FloorPlanJsonTest {
    private val plan = FloorPlanCatalog.plans.first()
    private val marker = RoomMarker("test", "Demo \"room\"\nA \\ coast", RoomMarkerType.Classroom, PlanPoint(.2f, .7f))
    private val encoded get() = FloorPlanJson.encode(plan, listOf(marker))

    @Test fun roundTripsEscapedNamesTypesAndNormalizedCoordinates() {
        assertEquals(listOf(marker), FloorPlanJson.decode(plan, encoded))
    }
    @Test fun emptyPlanRoundTrips() {
        assertTrue(FloorPlanJson.decode(plan, FloorPlanJson.encode(plan, emptyList())).isEmpty())
    }
    @Test fun otherPlanCannotReceiveThisDocumentsMarkers() {
        assertFails { FloorPlanJson.decode(FloorPlanCatalog.plans[1], encoded) }
    }
    @Test fun wrongBuildingRevisionSchemaOrDemoStatusIsRejected() {
        listOf(
            encoded.replace("sierra-hall", "gateway-hall"),
            encoded.replace("demo-1", "changed-revision"),
            encoded.replace("\"schemaVersion\": 1", "\"schemaVersion\": 9"),
            encoded.replace("\"demonstration\": true", "\"demonstration\": false")
        ).forEach { text -> assertFails { FloorPlanJson.decode(plan, text) } }
    }
    @Test fun duplicateIdsAreRejected() {
        assertFails { FloorPlanJson.encode(plan, listOf(marker, marker)) }
    }
    @Test fun unknownTypeIsRejected() {
        assertFails { FloorPlanJson.decode(plan, encoded.replace("classroom", "unknown")) }
    }
    @Test fun coordinatesOutsideImageAndNonFiniteValuesAreRejected() {
        assertFails { FloorPlanJson.decode(plan, encoded.replace("0.2", "1.2")) }
        assertFails { PlanPoint(Float.NaN, .5f) }
        assertFails { PlanPoint(.5f, Float.POSITIVE_INFINITY) }
        assertFails { PlanPoint(-.1f, .5f) }
    }
}
