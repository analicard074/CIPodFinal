package com.example.cipod_final

import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.location.LocationResult
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.NavigationStartState
import com.example.cipod_final.location.StartingPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NavigationStartStateTest {
    private val now = 1_800_000_000_000L
    private val fix = LocationReading(34.162197, -119.044662, 10.0, now, true)
    private fun ready(reading: LocationReading = fix): NavigationStartState {
        val state = NavigationStartState().beginRequest()
        return state.receive(state.requestId, LocationResult.Success(reading), now)
    }

    @Test fun startsWithoutRequestingLocationOrChoosingStart() {
        assertEquals(LocationStatus.Idle, NavigationStartState().status)
        assertNull(NavigationStartState().start)
    }
    @Test fun receivingLocationDoesNotAutomaticallyChooseIt() {
        assertNotNull(ready().reading)
        assertNull(ready().start)
    }
    @Test fun freshReadingCanBeExplicitlySelected() {
        assertEquals(StartingPoint.Device(fix), ready().useReading(now).start)
    }
    @Test fun approximateReadingIsVisibleButCannotBeSelected() {
        val state = ready(fix.copy(accuracyMeters = 100.0))
        assertNotNull(state.reading)
        assertNull(state.useReading(now).start)
    }
    @Test fun staleReadingCannotBeSelectedEvenIfExpiryTimerHasNotRun() {
        assertNull(ready().useReading(now + 30_000).start)
    }
    @Test fun onlyVerifiedBuildingsCanBeStartingPoints() {
        val state = NavigationStartState().chooseBuilding("sierra-hall")
        assertEquals(StartingPoint.Building("sierra-hall"), state.start)
        assertEquals(state, state.chooseBuilding("gateway-hall"))
        assertEquals(state, state.chooseBuilding("unknown"))
    }
    @Test fun manualSelectionReplacesDeviceStartAndRemovesReading() {
        val state = ready().useReading(now).chooseBuilding("broome-library")
        assertEquals(StartingPoint.Building("broome-library"), state.start)
        assertNull(state.reading)
    }
    @Test fun acquiringLocationPreservesManualChoiceUntilExplicitlyReplaced() {
        val state = NavigationStartState().chooseBuilding("sierra-hall").beginRequest()
        val result = state.receive(state.requestId, LocationResult.Success(fix), now)
        assertIs<StartingPoint.Building>(result.start)
        assertIs<StartingPoint.Device>(result.useReading(now).start)
    }
    @Test fun retryRemovesOldDeviceStartAndOldReading() {
        val state = ready().useReading(now).beginRequest()
        assertNull(state.start)
        assertNull(state.reading)
    }
    @Test fun cancelledRequestCannotApplyLateResult() {
        val pending = NavigationStartState().beginRequest()
        val cancelled = pending.cancel()
        assertEquals(LocationStatus.Cancelled, cancelled.status)
        assertEquals(cancelled, cancelled.receive(pending.requestId, LocationResult.Success(fix), now))
    }
    @Test fun manualSelectionWinsOverLateLocationCallback() {
        val pending = NavigationStartState().beginRequest()
        val manual = pending.chooseBuilding("sierra-hall")
        assertEquals(manual, manual.receive(pending.requestId, LocationResult.Success(fix), now))
    }
    @Test fun previousRequestCannotOverwriteRetry() {
        val first = NavigationStartState().beginRequest()
        val second = first.beginRequest()
        assertEquals(second, second.receive(first.requestId, LocationResult.Success(fix), now))
    }
    @Test fun expiryClearsDeviceStartButPreservesManualStart() {
        val device = ready().useReading(now).expire(now + 30_000)
        assertNull(device.start)
        assertEquals(LocationStatus.Stale, device.status)
        val manual = NavigationStartState().chooseBuilding("sierra-hall").beginRequest()
        val state = manual.receive(manual.requestId, LocationResult.Success(fix), now)
        assertIs<StartingPoint.Building>(state.expire(now + 30_000).start)
    }
    @Test fun clearDropsAllLocationDataAndInvalidatesPendingRequests() {
        val state = ready().useReading(now)
        val cleared = state.clear()
        assertNull(cleared.start)
        assertNull(cleared.reading)
        assertEquals(cleared, cleared.receive(state.requestId, LocationResult.Success(fix), now))
    }
    @Test fun permissionServicesTimeoutAndUnavailableFailuresPreserveManualStart() {
        listOf(LocationStatus.Denied, LocationStatus.ServicesDisabled, LocationStatus.TimedOut,
            LocationStatus.Unavailable).forEach { error ->
            val pending = NavigationStartState().chooseBuilding("sierra-hall").beginRequest()
            val failed = pending.receive(pending.requestId, LocationResult.Failure(error), now)
            assertEquals(error, failed.status)
            assertIs<StartingPoint.Building>(failed.start)
            assertNull(failed.reading)
        }
    }
    @Test fun invalidFixIsNotDisplayed() {
        val state = ready(fix.copy(accuracyMeters = -1.0))
        assertEquals(LocationStatus.Unavailable, state.status)
        assertNull(state.reading)
    }
}
