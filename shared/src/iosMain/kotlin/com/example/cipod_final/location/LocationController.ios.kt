@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.cipod_final.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
actual fun rememberLocationController(): LocationController {
    // Composition runs on the main thread, whose run loop delivers Core Location callbacks.
    val controller = remember { IosLocationController() }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    return controller
}

private class IosLocationController : LocationController {
    private val delegate = LocationDelegate(this)
    private val manager = CLLocationManager()
    private var permission: CancellableContinuation<Boolean>? = null
    private var reading: CancellableContinuation<LocationResult>? = null

    init {
        manager.delegate = delegate // Retain the delegate through the controller's composition lifetime.
        manager.desiredAccuracy = 25.0
        manager.allowsBackgroundLocationUpdates = false
    }

    private fun authorized(status: CLAuthorizationStatus): Boolean =
        status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways

    override fun locationServicesEnabled(): Boolean = CLLocationManager.locationServicesEnabled()

    override suspend fun requestPermission(): Boolean {
        val status = manager.authorizationStatus
        if (authorized(status)) return true
        if (status != kCLAuthorizationStatusNotDetermined) return false
        return suspendCancellableCoroutine { continuation ->
            permission = continuation
            continuation.invokeOnCancellation { if (permission === continuation) permission = null }
            manager.requestWhenInUseAuthorization()
        }
    }

    fun authorizationChanged(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusNotDetermined) return
        val pending = permission
        permission = null
        if (pending?.isActive == true) pending.resume(authorized(status))
        if (status == kCLAuthorizationStatusDenied || status == kCLAuthorizationStatusRestricted) {
            complete(LocationResult.Failure(LocationStatus.Denied))
        }
    }

    override suspend fun readCurrentLocation(): LocationResult {
        if (!CLLocationManager.locationServicesEnabled()) {
            return LocationResult.Failure(LocationStatus.ServicesDisabled)
        }
        if (!authorized(manager.authorizationStatus)) return LocationResult.Failure(LocationStatus.Denied)
        return suspendCancellableCoroutine { continuation ->
            reading = continuation
            continuation.invokeOnCancellation {
                if (reading === continuation) {
                    reading = null
                    manager.stopUpdatingLocation()
                }
            }
            manager.requestLocation()
        }
    }

    fun locationsUpdated(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val fix = didUpdateLocations.filterIsInstance<CLLocation>().maxByOrNull { it.timestamp.timeIntervalSince1970 }
        if (fix == null) {
            complete(LocationResult.Failure(LocationStatus.Unavailable))
            return
        }
        val point = fix.coordinate.useContents {
            LocationReading(
                latitude, longitude, fix.horizontalAccuracy,
                (fix.timestamp.timeIntervalSince1970 * 1000.0).toLong(),
                manager.accuracyAuthorization == CLAccuracyAuthorizationFullAccuracy
            )
        }
        complete(LocationResult.Success(point))
    }

    fun locationFailed(manager: CLLocationManager) {
        val status = when {
            !CLLocationManager.locationServicesEnabled() -> LocationStatus.ServicesDisabled
            !authorized(manager.authorizationStatus) -> LocationStatus.Denied
            else -> LocationStatus.Unavailable
        }
        complete(LocationResult.Failure(status))
    }

    private fun complete(result: LocationResult) {
        val pending = reading
        reading = null
        manager.stopUpdatingLocation()
        if (pending?.isActive == true) pending.resume(result)
    }

    override fun openAppSettings() {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }

    // iOS has no public deep link directly to the global Location Services pane.
    override fun openLocationSettings() = openAppSettings()

    fun dispose() {
        permission?.cancel()
        permission = null
        reading?.cancel()
        reading = null
        manager.stopUpdatingLocation()
        manager.delegate = null
    }
}

private class LocationDelegate(private val controller: IosLocationController) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        controller.authorizationChanged(manager)
    }
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        controller.locationsUpdated(manager, didUpdateLocations)
    }
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        controller.locationFailed(manager)
    }
}
