package com.example.cipod_final.navigationBar

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal actual fun formatEventDateTime(epochSeconds: Long, pattern: String): String {
    val formatter = SimpleDateFormat(pattern, Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("America/Los_Angeles")
    // Java dates use milliseconds, while Firestore timestamps use seconds
    val date = Date(epochSeconds * 1000)
    return formatter.format(date)
}
