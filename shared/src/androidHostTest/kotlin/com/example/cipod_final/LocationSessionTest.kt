package com.example.cipod_final

import com.example.cipod_final.location.LocationController
import com.example.cipod_final.location.LocationReading
import com.example.cipod_final.location.LocationResult
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.location.LocationStatus
import com.example.cipod_final.location.StartingPoint
import com.example.cipod_final.location.locationNowMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationSessionTest {
    private class FakeController : LocationController {
        var enabled = true
        var permissionCalls = 0
        var readingCalls = 0
        var cancelled = false
        val permission = CompletableDeferred<Boolean>()
        val result = CompletableDeferred<LocationResult>()
        override fun locationServicesEnabled() = enabled
        override suspend fun requestPermission(): Boolean {
            permissionCalls++
            return permission.await()
        }
        override suspend fun readCurrentLocation(): LocationResult {
            readingCalls++
            try { return result.await() } finally { cancelled = true }
        }
        override fun openAppSettings() = Unit
        override fun openLocationSettings() = Unit
    }

    @Test fun noAccessUntilActionAndPermissionThenOnlyOneReading() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val controller = FakeController()
            val session = LocationSession(scope)
            assertEquals(0, controller.permissionCalls)
            assertEquals(0, controller.readingCalls)
            session.requestLocation(controller)
            yield()
            assertEquals(1, controller.permissionCalls)
            assertEquals(0, controller.readingCalls)
            controller.permission.complete(true)
            yield()
            assertEquals(1, controller.readingCalls)
            controller.result.complete(LocationResult.Success(LocationReading(
                34.162197, -119.044662, 10.0, locationNowMillis(), true
            )))
            yield()
            assertEquals(LocationStatus.Ready, session.state.status)
            assertNull(session.state.start)
            session.useReading()
            assertIs<StartingPoint.Device>(session.state.start)
            assertEquals(1, controller.readingCalls)
        } finally { scope.cancel() }
    }

    @Test fun explicitRetryOfCachedFixProducesAnotherCenteringEvent() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val controller = FakeController().also {
                it.permission.complete(true)
                it.result.complete(LocationResult.Success(LocationReading(
                    34.162197, -119.044662, 10.0, locationNowMillis(), true
                )))
            }
            val session = LocationSession(scope)
            session.requestLocation(controller)
            yield()
            val snapshot = session.state.reading
            val revision = session.readingRevision
            session.requestLocation(controller)
            yield()
            assertEquals(snapshot, session.state.reading)
            assertEquals(revision + 1, session.readingRevision)
        } finally { scope.cancel() }
    }

    @Test fun deniedPermissionNeverReadsLocation() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val controller = FakeController().also { it.permission.complete(false) }
            val session = LocationSession(scope)
            session.requestLocation(controller)
            yield()
            assertEquals(LocationStatus.Denied, session.state.status)
            assertEquals(0, controller.readingCalls)
        } finally { scope.cancel() }
    }

    @Test fun disabledServicesDoNotPromptOrRead() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val controller = FakeController().also { it.enabled = false }
            val session = LocationSession(scope)
            session.requestLocation(controller)
            yield()
            assertEquals(LocationStatus.ServicesDisabled, session.state.status)
            assertEquals(0, controller.permissionCalls)
            assertEquals(0, controller.readingCalls)
        } finally { scope.cancel() }
    }

    @Test fun timeoutCancelsThePendingProviderRequest() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val controller = FakeController().also { it.permission.complete(true) }
            val session = LocationSession(scope, timeoutMillis = 10)
            session.requestLocation(controller)
            delay(100)
            assertEquals(LocationStatus.TimedOut, session.state.status)
            assertTrue(controller.cancelled)
        } finally { scope.cancel() }
    }

    @Test fun manualSelectionCancelsProviderAndCannotBeOverwritten() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val controller = FakeController().also { it.permission.complete(true) }
            val session = LocationSession(scope)
            session.requestLocation(controller)
            yield()
            session.chooseBuilding("sierra-hall")
            yield()
            assertTrue(controller.cancelled)
            controller.result.complete(LocationResult.Success(LocationReading(
                34.162197, -119.044662, 10.0, locationNowMillis(), true
            )))
            yield()
            assertEquals(StartingPoint.Building("sierra-hall"), session.state.start)
            assertNull(session.state.reading)
        } finally { scope.cancel() }
    }
}
