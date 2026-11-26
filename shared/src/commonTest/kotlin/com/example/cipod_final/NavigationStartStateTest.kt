package com.example.cipod_final

import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.location.*
import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.location.LocationResult
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.NavigationStartState
import com.example.cipod_final.location.StartingPoint
import kotlin.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

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
    @Test fun manualSelectionReplacesDeviceStartAndKeepsLatestReading() {
        val state = ready().useReading(now).chooseBuilding("broome-library")
        assertEquals(StartingPoint.Building("broome-library"), state.start)
        assertEquals(fix, state.reading)
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

@OptIn(ExperimentalCoroutinesApi::class)
class BuildingStartFallbackTest {
    private val now = 1_800_000_000_000L
    private val broome = assertNotNull(CampusBuildings.find("broome-library")?.coordinates)
    private fun weakFix() = LocationReading(broome.latitude, broome.longitude, 100.0, now, true)
    private fun state(reading: LocationReading = weakFix()) = NavigationStartState(
        reading = reading, status = LocationStatus.Ready)

    @Test fun weakFixUsesNearestBuildingAndRemovesImpreciseDeviceCoordinates() {
        CampusBuildings.all.filter { it.coordinates != null }.forEach { building ->
            val point = building.coordinates!!
            val weak = state(weakFix().copy(latitude = point.latitude, longitude = point.longitude))
            val fallback = weak.withNearbyBuildingFallback(now)
            assertEquals(StartingPoint.Building(building.id), fallback.start)
            assertNull(fallback.reading)
            assertEquals(LocationStatus.Idle, fallback.status)
        }
    }

    @Test fun manualBuildingAndGoodDeviceFixArePreserved() {
        val manual = state().copy(start = StartingPoint.Building("sierra-hall"))
        assertEquals(manual, manual.withNearbyBuildingFallback(now))
        val good = weakFix().copy(accuracyMeters = 5.0)
        val device = state(good).useReading(now)
        assertEquals(device, device.withNearbyBuildingFallback(now))
    }

    @Test fun unknownStaleOrDistantLocationsRequireAnExplicitBuildingChoice() {
        val stale = state(weakFix().copy(capturedAtMillis = now - LocationQualityPolicy.maxAgeMillis))
        val distant = state(weakFix().copy(latitude = 35.0))
        val uncertain = state(weakFix().copy(accuracyMeters = 1000.0))
        listOf(NavigationStartState(), stale, distant, uncertain).forEach {
            assertEquals(it, it.withNearbyBuildingFallback(now))
            assertNull(it.withNearbyBuildingFallback(now).start)
        }
    }

    @Test fun sessionAppliesFallbackAndManualSelectionClearsItsNotice() = runTest {
        val controller = object : LocationController {
            override fun locationServicesEnabled() = true
            override suspend fun requestPermission() = true
            override suspend fun readCurrentLocation(): LocationResult =
                LocationResult.Success(weakFix().copy(capturedAtMillis = locationNowMillis()))
            override fun openAppSettings() {}
            override fun openLocationSettings() {}
        }
        val session = LocationSession(this)
        session.requestLocation(controller)
        runCurrent()
        assertEquals(StartingPoint.Building("broome-library"), session.state.start)
        assertEquals("broome-library", session.fallbackBuildingId)
        assertNull(session.state.reading)
        session.chooseBuilding("sierra-hall")
        assertEquals(StartingPoint.Building("sierra-hall"), session.state.start)
        assertNull(session.fallbackBuildingId)
        session.clear()
    }

    @Test fun backgroundGpsRefreshesWithoutReplacingTheSelectedStartingBuilding() = runTest {
        var reads = 0
        val controller = object : LocationController {
            override fun locationServicesEnabled() = true
            override suspend fun requestPermission(): Boolean = error("Arrival checks must not prompt for permission")
            override suspend fun readCurrentLocation(): LocationResult {
                reads++
                return LocationResult.Success(LocationReading(broome.latitude + reads * .00001, broome.longitude,
                    5.0, locationNowMillis(), true))
            }
            override fun openAppSettings() {}
            override fun openLocationSettings() {}
        }
        val session = LocationSession(this)
        session.chooseBuilding("sierra-hall")
        val tracking = backgroundScope.launch { session.refreshAutomatically(controller) }
        runCurrent()
        assertEquals(StartingPoint.Building("sierra-hall"), session.state.start)
        assertNotNull(session.state.reading)
        assertEquals(broome.latitude + .00001, session.state.reading!!.latitude)
        assertEquals(1, reads)
        session.chooseBuilding("gateway-hall")
        advanceTimeBy(3_001)
        runCurrent()
        assertEquals(StartingPoint.Building("gateway-hall"), session.state.start)
        assertEquals(2, reads)
        val latest = assertNotNull(session.state.reading)
        assertEquals(broome.latitude + .00002, latest.latitude)
        val beforeSwitch = session.readingRevision
        session.selectCurrentLocation(controller)
        assertEquals(StartingPoint.Device(latest), session.state.start)
        assertEquals(beforeSwitch + 1, session.readingRevision)
        assertEquals(2, reads, "Use the most recent background fix without waiting for another request")
        advanceTimeBy(3_001)
        runCurrent()
        assertEquals(3, reads)
        assertEquals(broome.latitude + .00003, assertIs<StartingPoint.Device>(session.state.start).reading.latitude)
        tracking.cancel()
        runCurrent()
        session.clear()
    }

    @Test fun choosingMyLocationRequestsANewFixWhenNoUsableBackgroundReadingExists() = runTest {
        var permissionRequests = 0
        var reads = 0
        val controller = object : LocationController {
            override fun locationServicesEnabled() = true
            override suspend fun requestPermission(): Boolean { permissionRequests++; return true }
            override suspend fun readCurrentLocation(): LocationResult {
                reads++
                return LocationResult.Success(LocationReading(broome.latitude, broome.longitude,
                    5.0, locationNowMillis(), true))
            }
            override fun openAppSettings() {}
            override fun openLocationSettings() {}
        }
        val session = LocationSession(this)
        session.chooseBuilding("sierra-hall")
        session.selectCurrentLocation(controller)
        runCurrent()
        assertEquals(1, permissionRequests)
        assertEquals(1, reads)
        assertEquals(broome.latitude, assertIs<StartingPoint.Device>(session.state.start).reading.latitude)
        session.clear()
    }

    @Test fun backgroundChecksRemainSilentUntilMyLocationIsSelected() = runTest {
        val pending = CompletableDeferred<LocationResult>()
        var permissionRequests = 0
        val controller = object : LocationController {
            override fun locationServicesEnabled() = true
            override suspend fun requestPermission(): Boolean { permissionRequests++; return true }
            override suspend fun readCurrentLocation(): LocationResult = pending.await()
            override fun openAppSettings() {}
            override fun openLocationSettings() {}
        }
        val session = LocationSession(this)
        session.chooseBuilding("gateway-hall")
        val tracking = backgroundScope.launch { session.refreshAutomatically(controller) }
        runCurrent()
        assertTrue(session.state.busy)
        assertFalse(session.isRequestingLocation)
        assertFalse(session.locationFeedbackRequested)
        assertEquals(0, permissionRequests)
        assertEquals(StartingPoint.Building("gateway-hall"), session.state.start)

        session.selectCurrentLocation(controller)
        runCurrent()
        assertTrue(session.isRequestingLocation)
        assertTrue(session.locationFeedbackRequested)
        assertEquals(1, permissionRequests)
        pending.complete(LocationResult.Success(LocationReading(broome.latitude, broome.longitude,
            5.0, locationNowMillis(), true)))
        runCurrent()
        assertFalse(session.isRequestingLocation)
        assertIs<StartingPoint.Device>(session.state.start)
        session.chooseBuilding("sierra-hall")
        assertFalse(session.locationFeedbackRequested)
        tracking.cancel()
        runCurrent()
        session.clear()
    }
}
