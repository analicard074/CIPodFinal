package com.example.cipod_final.location

import com.example.cipod_final.buildings.CampusBuildings

sealed interface StartingPoint {
    data class Building(val buildingId: String) : StartingPoint
    data class Device(val reading: LocationReading) : StartingPoint
}

/** This state is remembered in RAM, never placed in rememberSaveable or persistent storage. */
data class NavigationStartState(
    val start: StartingPoint? = null,
    val reading: LocationReading? = null,
    val status: LocationStatus = LocationStatus.Idle,
    val requestId: Long = 0
) {
    val busy: Boolean get() = status == LocationStatus.RequestingPermission || status == LocationStatus.Locating

    fun beginRequest(): NavigationStartState = copy(
        start = start.takeUnless { it is StartingPoint.Device },
        reading = null, status = LocationStatus.RequestingPermission, requestId = requestId + 1
    )

    fun receive(id: Long, result: LocationResult, nowMillis: Long): NavigationStartState {
        if (id != requestId || !busy) return this
        return when (result) {
            is LocationResult.Failure -> copy(status = result.status)
            is LocationResult.Success -> when (LocationQualityPolicy.evaluate(result.reading, nowMillis)) {
                LocationQuality.Invalid -> copy(reading = null, status = LocationStatus.Unavailable)
                LocationQuality.Stale -> copy(reading = result.reading, status = LocationStatus.Stale)
                else -> copy(reading = result.reading, status = LocationStatus.Ready)
            }
        }
    }

    fun useReading(nowMillis: Long): NavigationStartState {
        val snapshot = reading ?: return this
        return if (LocationQualityPolicy.evaluate(snapshot, nowMillis) == LocationQuality.Usable) {
            copy(start = StartingPoint.Device(snapshot))
        } else expire(nowMillis)
    }

    fun chooseBuilding(id: String): NavigationStartState =
        if (CampusBuildings.find(id)?.coordinates == null) this else copy(
            start = StartingPoint.Building(id), reading = null,
            status = LocationStatus.Idle, requestId = requestId + 1
        )

    fun clear(): NavigationStartState = NavigationStartState(requestId = requestId + 1)

    fun cancel(): NavigationStartState = copy(
        status = if (busy) LocationStatus.Cancelled else status, requestId = requestId + 1
    )

    fun expire(nowMillis: Long): NavigationStartState {
        val snapshot = reading ?: return this
        return if (LocationQualityPolicy.evaluate(snapshot, nowMillis) == LocationQuality.Stale) copy(
            start = start.takeUnless { it is StartingPoint.Device }, status = LocationStatus.Stale
        ) else this
    }
}
