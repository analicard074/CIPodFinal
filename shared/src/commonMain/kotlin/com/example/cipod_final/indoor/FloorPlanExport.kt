package com.example.cipod_final.indoor

import androidx.compose.runtime.Composable

/** Copies only explicitly exported plan-marker JSON, never GPS coordinates. */
@Composable
expect fun rememberFloorPlanExporter(): (String) -> Boolean
