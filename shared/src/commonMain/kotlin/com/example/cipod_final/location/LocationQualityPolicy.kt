package com.example.cipod_final.location

object LocationQualityPolicy {
    const val targetAccuracyMeters = 25.0
    const val maxAgeMillis = 30_000L
    const val requestTimeoutMillis = 20_000L

    fun evaluate(reading: LocationReading, nowMillis: Long): LocationQuality {
        if (!reading.latitude.isFinite() || reading.latitude !in -90.0..90.0 ||
            !reading.longitude.isFinite() || reading.longitude !in -180.0..180.0 ||
            !reading.accuracyMeters.isFinite() || reading.accuracyMeters <= 0.0 ||
            reading.capturedAtMillis <= 0L
        ) return LocationQuality.Invalid
        val age = nowMillis - reading.capturedAtMillis
        if (age < -5_000L || age >= maxAgeMillis) return LocationQuality.Stale
        return if (reading.precisePermission && reading.accuracyMeters <= targetAccuracyMeters) {
            LocationQuality.Usable
        } else LocationQuality.Approximate
    }
}
