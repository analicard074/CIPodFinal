package com.example.cipod_final.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalkingRouteStateTest {
    @Test
    fun routeIsNotRequestedUntilStudentExplicitlyTapsFindRoute() {
        val state = WalkingRouteState()
        assertFalse(state.routeRequested)
        assertEquals(0, state.requestCount)

        state.requestRoute()

        assertTrue(state.routeRequested)
        assertEquals(1, state.requestCount)
    }
}
