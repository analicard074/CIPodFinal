package com.example.cipod_final.navigationBar

// Always show campus Pacific time, including PST/PDT, even when a student's phone is elsewhere.
internal expect fun formatEventDateTime(
    epochSeconds: Long,
    pattern: String = "EEE, MMM d, yyyy · h:mm a z",
): String

internal fun formatEventSchedule(startSeconds: Long, endSeconds: Long): String {
    val startDate = formatEventDateTime(startSeconds, "EEE, MMM d, yyyy")
    val endDate = formatEventDateTime(endSeconds, "EEE, MMM d, yyyy")
    val dateRange = if (startDate == endDate) startDate else "$startDate - $endDate"

    val startZone = formatEventDateTime(startSeconds, "z")
    val endZone = formatEventDateTime(endSeconds, "z")
    // Include both timezone labels when an event spans a daylight-saving change
    val startTime = formatEventDateTime(startSeconds, "h:mm a")
    val endTime = formatEventDateTime(endSeconds, "h:mm a")
    val timeRange = if (startZone == endZone) {
        "$startTime - $endTime $endZone"
    } else {
        "$startTime $startZone - $endTime $endZone"
    }
    return "$dateRange\n$timeRange"
}
