package com.example.cipod_final.location

/** Volatile snapshot only. Never serialize, save, log, or send this object to a server. */
data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val capturedAtMillis: Long,
    val precisePermission: Boolean
) {
    // Avoid exposing coordinates through incidental debug string interpolation.
    override fun toString(): String = "LocationReading(redacted)"
}

enum class LocationQuality { Invalid, Stale, Approximate, Usable }
enum class LocationStatus {
    Idle, RequestingPermission, Locating, Ready, Denied, ServicesDisabled,
    Unavailable, TimedOut, Cancelled, Stale
}

sealed interface LocationResult {
    data class Success(val reading: LocationReading) : LocationResult
    data class Failure(val status: LocationStatus) : LocationResult
}
