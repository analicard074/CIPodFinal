package com.example.cipod_final.location

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

fun locationNowMillis(): Long = Clock.System.now().toEpochMilliseconds()

// Manages location requests and the selected starting point
class LocationSession(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = LocationQualityPolicy.requestTimeoutMillis
) {
    var state by mutableStateOf(NavigationStartState())
        private set

    // Incrementing this tells the map to center on the location again
    var readingRevision by mutableStateOf(0L)
        private set

    private var locationRequestJob: Job? = null
    private var locationExpiryJob: Job? = null
    private var automaticRefreshEnabled = true
    private var startingPointCleared = false

    suspend fun refreshAutomatically(controller: LocationController) {
        automaticRefreshEnabled = !startingPointCleared && state.start !is StartingPoint.Building
        var isFirstAttempt = true
        state = state.expire(locationNowMillis())
        try {
            while (currentCoroutineContext().isActive) {
                val requestInProgress = locationRequestJob?.isActive == true || state.busy
                val buildingSelected = state.start is StartingPoint.Building
                val canTryPermission = isFirstAttempt || state.status != LocationStatus.Denied

                if (automaticRefreshEnabled && !requestInProgress && !buildingSelected && canTryPermission) {
                    requestLocation(controller, promptForPermission = false, automatic = true)
                    locationRequestJob?.join()
                }
                isFirstAttempt = false
                delay(3.seconds)
            }
        } finally {
            cancelRequest()
        }
    }

    fun requestLocation(
        controller: LocationController,
        promptForPermission: Boolean = true,
        automatic: Boolean = false
    ) {
        cancelRequest()
        startingPointCleared = false
        automaticRefreshEnabled = true
        val previousState = state.expire(locationNowMillis())
        val isBackgroundRefresh =
            automatic && previousState.reading != null && previousState.status != LocationStatus.Stale
        if (!isBackgroundRefresh) {
            locationExpiryJob?.cancel()
        }
        // Keep the current location visible during automatic updates
        state = if (isBackgroundRefresh) {
            previousState.copy(requestId = previousState.requestId + 1)
        } else {
            state.beginRequest()
        }
        val requestId = state.requestId
        fun handleResult(result: LocationResult) {
            applyLocationResult(
                requestId,
                result,
                moveCamera = !automatic,
                isBackgroundRefresh = isBackgroundRefresh
            )
        }
        locationRequestJob = scope.launch {
            try {
                if (!controller.locationServicesEnabled()) {
                    handleResult(LocationResult.Failure(LocationStatus.ServicesDisabled))
                    return@launch
                }
                if (promptForPermission && !controller.requestPermission()) {
                    handleResult(LocationResult.Failure(LocationStatus.Denied))
                    return@launch
                }
                if (state.requestId != requestId) {
                    return@launch
                }
                if (!isBackgroundRefresh) {
                    state = state.copy(status = LocationStatus.Locating)
                }
                val result = withTimeout(timeoutMillis) {
                    controller.readCurrentLocation()
                }
                handleResult(result)
            } catch (_: TimeoutCancellationException) {
                handleResult(LocationResult.Failure(LocationStatus.TimedOut))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Avoid logging errors that could contain location data.
                handleResult(LocationResult.Failure(LocationStatus.Unavailable))
            }
        }
    }

    private fun applyLocationResult(
        requestId: Long,
        result: LocationResult,
        moveCamera: Boolean,
        isBackgroundRefresh: Boolean
    ) {
        if (state.requestId != requestId) {
            return
        }
        if (!isBackgroundRefresh && !state.busy) {
            return
        }
        val updatedState =
            state.copy(status = LocationStatus.Locating)
                .receive(requestId, result, locationNowMillis())
        val hasTemporaryFailure = when (updatedState.status) {
            LocationStatus.TimedOut, LocationStatus.Unavailable, LocationStatus.Stale -> true
            else -> false
        }
        if (isBackgroundRefresh && hasTemporaryFailure) {
            state = state.expire(locationNowMillis())
            scheduleExpiry()
            return
        }
        state = updatedState
        if (result is LocationResult.Success) {
            // Center once automatically; later automatic readings only move the location marker.
            val shouldCenterMap = moveCamera || readingRevision == 0L
            if (state.reading != null && shouldCenterMap) {
                readingRevision++
            }
            if (state.start is StartingPoint.Device) {
                state = state.copy(start = null)
            }
            state = state.useReading(locationNowMillis())
        }
        state = state.expire(locationNowMillis())
        scheduleExpiry()
    }

    private fun scheduleExpiry() {
        locationExpiryJob?.cancel()
        val reading = state.reading ?: return
        if (state.status == LocationStatus.Stale) {
            return
        }

        locationExpiryJob = scope.launch {
            val expiresAtMillis = reading.capturedAtMillis + LocationQualityPolicy.maxAgeMillis
            val remainingMillis = expiresAtMillis - locationNowMillis()
            delay(remainingMillis.coerceAtLeast(1L))
            if (state.reading == reading) {
                state = state.expire(locationNowMillis())
            }
        }
    }

    fun useReading() {
        state = state.useReading(locationNowMillis())
    }

    fun chooseBuilding(id: String) {
        val selectedState = state.chooseBuilding(id)
        if (selectedState == state) {
            return
        }
        cancelRequest()
        locationExpiryJob?.cancel()
        state = selectedState
    }

    fun clear() {
        startingPointCleared = true
        cancelRequest()
        locationExpiryJob?.cancel()
        state = state.clear()
    }

    fun cancelRequest() {
        automaticRefreshEnabled = false
        state = state.cancel()
        locationRequestJob?.cancel()
        locationRequestJob = null
        scheduleExpiry()
    }
}
