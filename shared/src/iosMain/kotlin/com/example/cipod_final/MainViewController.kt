package com.example.cipod_final

import androidx.compose.ui.window.ComposeUIViewController
import com.example.cipod_final.indoor.IndoorEditorPolicy

fun MainViewController(debugBuild: Boolean = false, indoorEditorRequested: Boolean = false) = ComposeUIViewController {
    App(indoorEditorEnabled = IndoorEditorPolicy.allowed(debugBuild, indoorEditorRequested))
}