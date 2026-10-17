package com.example.cipod_final.location

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock

fun locationNowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Owns cancellable work and volatile UI state, without collecting a location stream. */
class LocationSession(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = LocationQualityPolicy.requestTimeoutMillis
) {
    var state by mutableStateOf(NavigationStartState())
        private set
    // A successful retry can return the same cached snapshot; still recenter once for that action.
    var readingRevision by mutableStateOf(0L)
        private set
    private var request: Job? = null
    private var expiry: Job? = null

    fun requestLocation(controller: LocationController) {
        cancelRequest()
        expiry?.cancel()
        state = state.beginRequest()
        val id = state.requestId
        request = scope.launch {
            try {
                if (!controller.locationServicesEnabled()) {
                    receive(id, LocationResult.Failure(LocationStatus.ServicesDisabled))
                    return@launch
                }
                if (!controller.requestPermission()) {
                    receive(id, LocationResult.Failure(LocationStatus.Denied))
                    return@launch
                }
                if (state.requestId != id) return@launch
                state = state.copy(status = LocationStatus.Locating)
                val result = withTimeout(timeoutMillis) {
                    controller.readCurrentLocation()
                }
                receive(id, result)
            } catch (_: TimeoutCancellationException) {
                receive(id, LocationResult.Failure(LocationStatus.TimedOut))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not log OS errors that could contain sensitive location payloads.
                receive(id, LocationResult.Failure(LocationStatus.Unavailable))
            }
        }
    }

    private fun receive(id: Long, result: LocationResult) {
        if (state.requestId != id || !state.busy) return
        state = state.receive(id, result, locationNowMillis())
        if (result is LocationResult.Success && state.reading != null) readingRevision++
        val snapshot = state.reading ?: return
        if (state.status != LocationStatus.Ready) return
        expiry?.cancel()
        expiry = scope.launch {
            delay((snapshot.capturedAtMillis + LocationQualityPolicy.maxAgeMillis - locationNowMillis())
                .coerceAtLeast(1L))
            if (state.reading == snapshot) state = state.expire(locationNowMillis())
        }
    }

    fun useReading() { state = state.useReading(locationNowMillis()) }

    fun chooseBuilding(id: String) {
        val selected = state.chooseBuilding(id)
        if (selected == state) return
        cancelRequest()
        expiry?.cancel()
        state = selected
    }

    fun clear() {
        cancelRequest()
        expiry?.cancel()
        state = state.clear()
    }

    fun cancelRequest() {
        state = state.cancel()
        request?.cancel()
        request = null
    }

    fun refreshFreshness() { state = state.expire(locationNowMillis()) }
}
