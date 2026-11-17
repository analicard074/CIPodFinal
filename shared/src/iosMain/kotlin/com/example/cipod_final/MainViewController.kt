package com.example.cipod_final

import androidx.compose.ui.window.ComposeUIViewController
import com.example.cipod_final.indoor.IndoorEditorPolicy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.UIKit.UIViewController

fun MainViewController(
    debugBuild: Boolean = false,
    indoorEditorRequested: Boolean = false,
): UIViewController {
    val locationManager = CLLocationManager()
    return ComposeUIViewController {
        App(
            indoorEditorEnabled = IndoorEditorPolicy.allowed(debugBuild, indoorEditorRequested),
            onLoginSuccess = {
                // Authorization only; Directions owns on-demand location readings.
                if (locationManager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
                    locationManager.requestWhenInUseAuthorization()
                }
            }
        )
    }
}
