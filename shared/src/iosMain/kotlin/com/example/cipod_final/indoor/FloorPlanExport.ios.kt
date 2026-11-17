package com.example.cipod_final.indoor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberFloorPlanExporter(): (String) -> Boolean = remember {
    { text: String -> runCatching { UIPasteboard.generalPasteboard.string = text; true }.getOrDefault(false) }
}
