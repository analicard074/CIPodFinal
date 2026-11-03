package com.example.cipod_final

import androidx.compose.ui.window.ComposeUIViewController
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val locationManager = CLLocationManager()
    return ComposeUIViewController {
        App(onLoginSuccess = {
            if (locationManager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
                locationManager.requestWhenInUseAuthorization()
            }
        })
    }
}