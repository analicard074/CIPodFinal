package com.example.cipod_final

import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.indoor.IndoorEditorPolicy
import com.example.cipod_final.indoor.PlanPoint
import com.example.cipod_final.indoor.RoomMarker
import com.example.cipod_final.indoor.RoomMarkerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FloorPlanEditorTest {
    private val plan = FloorPlanCatalog.plans.first()
    private val sample = RoomMarker("example", "Demo classroom A", RoomMarkerType.Classroom, PlanPoint(.2f, .3f))
    private fun editor(): FloorPlanEditorState = FloorPlanEditorState(true).also {
        it.load(plan, listOf(sample)); it.enableEditing(true)
    }

    @Test fun normalBuildCannotEnterEditingMutateOrExport() {
        val state = FloorPlanEditorState()
        state.load(plan, listOf(sample))
        state.enableEditing(true)
        assertFalse(state.editing)
        assertNull(state.add(plan, RoomMarkerType.Restroom, PlanPoint(.5f, .5f)))
        assertFalse(state.move(plan, sample.id, PlanPoint(.9f, .9f)))
        assertFalse(state.update(plan, sample.id, "Changed", RoomMarkerType.Entrance))
        assertFalse(state.delete(plan, sample.id))
        assertNull(state.export(plan))
        assertEquals(listOf(sample), state.markers(plan))
    }
    @Test fun releaseCannotBeEnabledEvenWhenRequested() {
        assertFalse(IndoorEditorPolicy.allowed(false, true))
        assertFalse(IndoorEditorPolicy.allowed(false, false))
        assertFalse(IndoorEditorPolicy.allowed(true, false))
        assertTrue(IndoorEditorPolicy.allowed(true, true))
    }
    @Test fun developerStartsInReadOnlyMode() {
        val state = FloorPlanEditorState(true)
        state.load(plan, emptyList())
        assertFalse(state.editing)
        assertNull(state.add(plan, RoomMarkerType.Stair, PlanPoint(.5f, .5f)))
    }
    @Test fun supportsEveryRequestedMarkerTypeWithUniqueIds() {
        val state = editor()
        val added = RoomMarkerType.entries.map { assertNotNull(state.add(plan, it, PlanPoint(.5f, .5f))) }
        assertEquals(RoomMarkerType.entries.toList(), added.map { it.type })
        assertEquals(added.size, added.map { it.id }.distinct().size)
        assertTrue(added.all { it.name.startsWith("Demo ") })
        assertTrue(state.dirty(plan))
    }
    @Test fun moveRenameChangeTypeAndDeleteAffectOnlySelectedMarker() {
        val state = editor()
        val other = assertNotNull(state.add(plan, RoomMarkerType.Elevator, PlanPoint(.6f, .6f)))
        assertTrue(state.move(plan, sample.id, PlanPoint(.8f, .9f)))
        assertTrue(state.update(plan, sample.id, "  Demo entrance renamed  ", RoomMarkerType.Entrance))
        val changed = state.markers(plan).first { it.id == sample.id }
        assertEquals(PlanPoint(.8f, .9f), changed.position)
        assertEquals("Demo entrance renamed", changed.name)
        assertEquals(RoomMarkerType.Entrance, changed.type)
        assertEquals(other, state.markers(plan).first { it.id == other.id })
        assertTrue(state.delete(plan, sample.id))
        assertEquals(listOf(other), state.markers(plan))
    }
    @Test fun switchingPlansAndReloadingCannotOverwriteDrafts() {
        val state = editor()
        state.move(plan, sample.id, PlanPoint(.9f, .9f))
        state.load(FloorPlanCatalog.plans[1], emptyList())
        state.load(plan, listOf(sample))
        assertEquals(PlanPoint(.9f, .9f), state.markers(plan).single().position)
        assertTrue(state.markers(FloorPlanCatalog.plans[1]).isEmpty())
    }
    @Test fun readOnlyPreviewBlocksEditsButKeepsDraft() {
        val state = editor()
        state.move(plan, sample.id, PlanPoint(.9f, .9f))
        state.enableEditing(false)
        assertFalse(state.delete(plan, sample.id))
        assertEquals(PlanPoint(.9f, .9f), state.markers(plan).single().position)
    }
    @Test fun invalidNamesAndMissingMarkersDoNotChangeData() {
        val state = editor()
        assertFalse(state.update(plan, sample.id, "   ", RoomMarkerType.Stair))
        assertFalse(state.update(plan, sample.id, "x".repeat(81), RoomMarkerType.Stair))
        assertFalse(state.delete(plan, "missing"))
        assertEquals(listOf(sample), state.markers(plan))
    }
}
