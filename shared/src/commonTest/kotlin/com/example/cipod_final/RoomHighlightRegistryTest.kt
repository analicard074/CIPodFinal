package com.example.cipod_final

import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.PlanPoint
import com.example.cipod_final.indoor.PlanTransform
import com.example.cipod_final.indoor.RoomDirectory
import com.example.cipod_final.indoor.RoomDirectoryState
import com.example.cipod_final.indoor.RoomCategory
import com.example.cipod_final.indoor.RoomHighlight
import com.example.cipod_final.indoor.RoomHighlightRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoomHighlightRegistryTest {
    private val secondFloor = FloorPlanCatalog.plans.single {
        it.id == "broome-library-second-floor-draft"
    }
    private val room2330 = RoomDirectory.find("broome-library", "broome-library-2330")!!

    @Test
    fun classroom2330ResolvesForItsCurrentFloorAndRevision() {
        assertEquals("broome-library-second-floor-draft", room2330.floorPlanId)
        assertEquals("Classroom", room2330.publishedType)
        assertEquals(RoomCategory.CLASSROOM, room2330.category)
        assertNotNull(
            RoomHighlightRegistry.find(room2330.id, secondFloor.id, secondFloor.revision)
        )
    }

    @Test
    fun highlightDoesNotResolveForAnotherFloorOrRevision() {
        val highlight = RoomHighlightRegistry.find(
            room2330.id, secondFloor.id, secondFloor.revision
        )
        assertNotNull(highlight)
        assertNull(
            RoomHighlightRegistry.find(
                room2330.id, "broome-library-first-floor-draft", secondFloor.revision
            )
        )
        assertNull(RoomHighlightRegistry.find(room2330.id, secondFloor.id, "reference-draft-2"))
        assertNull(RoomHighlightRegistry.find("sierra-hall-2330", secondFloor.id, secondFloor.revision))
    }

    @Test
    fun highlightCoordinatesAllowNormalizedEdgesAndRejectInvalidBounds() {
        RoomHighlight(
            roomId = "edge-case",
            floorPlanId = "test-plan",
            planRevision = "r1",
            left = 0f,
            top = 0f,
            right = 1f,
            bottom = 1f
        )
        val valid = RoomHighlightRegistry.highlights.single()
        assertEquals(0.133987f, valid.left)
        assertEquals(0.418100f, valid.top)
        assertEquals(0.235294f, valid.right)
        assertEquals(0.524887f, valid.bottom)

        assertFailsWith<IllegalArgumentException> {
            RoomHighlight("bad", "test-plan", "r1", -0.01f, 0f, 0.5f, 0.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            RoomHighlight("bad", "test-plan", "r1", 0f, 0f, 1.01f, 0.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            RoomHighlight("bad", "test-plan", "r1", 0.5f, 0.5f, 0.5f, 0.8f)
        }
    }

    @Test
    fun clearingOrChangingSelectionRemovesClassroom2330Highlight() {
        val selected = RoomDirectoryState("broome-library").select(room2330.id)
        assertEquals(room2330.id, selected.selectedRoom?.id)
        assertNotNull(
            selected.selectedRoom?.let {
                RoomHighlightRegistry.find(it.id, secondFloor.id, secondFloor.revision)
            }
        )

        val changed = selected.select("broome-library-2325")
        assertEquals("broome-library-2325", changed.selectedRoom?.id)
        assertNull(
            changed.selectedRoom?.let {
                RoomHighlightRegistry.find(it.id, secondFloor.id, secondFloor.revision)
            }
        )

        val cleared = selected.clearSelection()
        assertNull(cleared.selectedRoom)
    }

    @Test
    fun highlightBoundsStayInImageSpaceThroughZoomPanAndReset() {
        val highlight = RoomHighlightRegistry.find(
            room2330.id, secondFloor.id, secondFloor.revision
        )!!
        val fit = PlanTransform(390f, 820f, secondFloor.aspectRatio)
        val moved = fit.gesture(2.3f, -28f, 37f, 195f, 410f)
        val corners = listOf(
            PlanPoint(highlight.left, highlight.top),
            PlanPoint(highlight.right, highlight.top),
            PlanPoint(highlight.left, highlight.bottom),
            PlanPoint(highlight.right, highlight.bottom)
        )
        corners.forEach { point ->
            val (x, y) = moved.screen(point)
            val restored = moved.normalized(x, y)
            assertNotNull(restored)
            assertEquals(point.x, restored.x, 0.0001f)
            assertEquals(point.y, restored.y, 0.0001f)
        }

        val reset = moved.copy(zoom = 1.2f, panX = 0f, panY = 0f)
        val (resetLeft, resetTop) = reset.screen(PlanPoint(highlight.left, highlight.top))
        val (fitLeft, fitTop) = fit.copy(zoom = 1.2f).screen(PlanPoint(highlight.left, highlight.top))
        assertEquals(fitLeft, resetLeft, 0.0001f)
        assertEquals(fitTop, resetTop, 0.0001f)
    }
}
