package com.example.cipod_final.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.CancellationSignal
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberLocationController(): LocationController {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val controller = remember(context, activity) { AndroidLocationController(context, activity) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        controller.permissionResult()
    }
    SideEffect {
        controller.launchPermission = {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    return controller
}

private class AndroidLocationController(private val context: Context, private val activity: Activity?) : LocationController {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var launchPermission: (() -> Unit)? = null
    private var permission: CancellableContinuation<Boolean>? = null

    private fun granted(name: String): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED

    private fun canLocate(): Boolean = granted(Manifest.permission.ACCESS_COARSE_LOCATION) ||
        granted(Manifest.permission.ACCESS_FINE_LOCATION)

    override fun locationServicesEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(manager)

    override suspend fun requestPermission(): Boolean {
        if (canLocate()) return true
        if (activity == null) return false // Includes unsupported preview/host environments.
        return suspendCancellableCoroutine { continuation ->
            permission = continuation
            continuation.invokeOnCancellation { if (permission === continuation) permission = null }
            val launch = launchPermission
            if (launch == null) {
                permission = null
                continuation.resume(false)
            } else launch()
        }
    }

    fun permissionResult() {
        val pending = permission
        permission = null
        if (pending?.isActive == true) pending.resume(canLocate())
    }

    override suspend fun readCurrentLocation(): LocationResult {
        if (!canLocate()) return LocationResult.Failure(LocationStatus.Denied)
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            return LocationResult.Failure(LocationStatus.ServicesDisabled)
        }
        val precise = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val provider = when {
            precise && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return LocationResult.Failure(LocationStatus.Unavailable)
        }
        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager, provider, cancellation, ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (!continuation.isActive) return@getCurrentLocation
                    val result = if (location == null) LocationResult.Failure(LocationStatus.Unavailable) else {
                        val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
                        LocationResult.Success(LocationReading(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy.toDouble() else -1.0,
                            capturedAtMillis = locationNowMillis() - ageMillis,
                            precisePermission = precise
                        ))
                    }
                    continuation.resume(result)
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(LocationResult.Failure(LocationStatus.Denied))
            } catch (_: IllegalArgumentException) {
                if (continuation.isActive) continuation.resume(LocationResult.Failure(LocationStatus.Unavailable))
            }
        }
    }

    override fun openAppSettings() {
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    override fun openLocationSettings() { open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }

    private fun open(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun dispose() {
        permission?.cancel()
        permission = null
        launchPermission = null
    }
}
