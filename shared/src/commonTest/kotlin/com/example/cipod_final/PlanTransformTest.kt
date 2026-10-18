package com.example.cipod_final

import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.PlanPoint
import com.example.cipod_final.indoor.PlanTransform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanTransformTest {
    private fun assertPoint(expected: PlanPoint, actual: PlanPoint?) {
        assertNotNull(actual)
        assertEquals(expected.x, actual.x, .0001f)
        assertEquals(expected.y, actual.y, .0001f)
    }
    @Test fun normalizedCoordinatesRoundTripWithLetterboxingZoomAndPan() {
        val t = PlanTransform(400f, 800f, 1.4f).gesture(3f, -30f, 50f, 200f, 400f)
        val p = PlanPoint(.25f, .7f)
        val (x, y) = t.screen(p)
        assertPoint(p, t.normalized(x, y))
    }
    @Test fun tappingLetterboxDoesNotPlaceAMarker() {
        val t = PlanTransform(400f, 800f, 1.4f)
        assertNull(t.normalized(200f, 0f))
    }
    @Test fun draggingUsesScaledPlanSizeAndClampsAtEdges() {
        val t = PlanTransform(400f, 800f, 1.4f, zoom = 2f)
        assertPoint(PlanPoint(.6f, .5f), t.drag(PlanPoint(.5f, .5f), 80f, 0f))
        assertPoint(PlanPoint(1f, 0f), t.drag(PlanPoint(.5f, .5f), 10000f, -10000f))
    }
    @Test fun resizingPreservesNormalizedPosition() {
        val t = PlanTransform(400f, 800f, 1.4f, zoom = 3f, panX = 30f).resized(900f, 400f)
        val p = PlanPoint(.1f, .9f)
        val (x, y) = t.screen(p)
        assertPoint(p, t.normalized(x, y))
    }
    @Test fun zoomIsBoundedAndFitViewCannotBePannedAway() {
        val t = PlanTransform(400f, 800f, 1.4f)
        assertEquals(6f, t.gesture(100f, 0f, 0f, 200f, 400f).zoom)
        val reset = t.gesture(.01f, 1000f, 1000f, 200f, 400f)
        assertEquals(1f, reset.zoom)
        assertEquals(0f, reset.panX)
        assertEquals(0f, reset.panY)
    }
    @Test fun zoomKeepsTheGestureAnchorWhenBoundsAllowIt() {
        val t = PlanTransform(400f, 400f, 1f, zoom = 2f)
        val anchor = t.normalized(180f, 210f)!!
        val next = t.gesture(1.2f, 0f, 0f, 180f, 210f)
        assertPoint(anchor, next.normalized(180f, 210f))
    }
    @Test fun catalogOnlyContainsExplicitFictionalLayouts() {
        val plans = FloorPlanCatalog.forBuilding("sierra-hall")
        assertEquals(listOf("Demo layout A", "Demo layout B"), plans.map { it.label })
        assertTrue(plans.all { it.isDemonstration && it.sourceUrl == null })
        assertTrue(FloorPlanCatalog.forBuilding("gateway-hall").isEmpty())
        assertTrue(FloorPlanCatalog.forBuilding("broome-library").isEmpty())
    }
}
