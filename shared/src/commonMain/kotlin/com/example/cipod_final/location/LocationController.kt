package com.example.cipod_final.location

import androidx.compose.runtime.Composable

interface LocationController {
    fun locationServicesEnabled(): Boolean
    /** Called only from an explicit Use My Location/Retry action. */
    suspend fun requestPermission(): Boolean
    /** Exactly one reading; the caller supplies a timeout and cancels on leaving the foreground. */
    suspend fun readCurrentLocation(): LocationResult
    fun openAppSettings()
    fun openLocationSettings()
}

@Composable
expect fun rememberLocationController(): LocationController
