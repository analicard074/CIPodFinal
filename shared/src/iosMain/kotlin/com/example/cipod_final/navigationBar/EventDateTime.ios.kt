package com.example.cipod_final.navigationBar

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName

internal actual fun formatEventDateTime(epochSeconds: Long, pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale("en_US_POSIX")
    formatter.timeZone = checkNotNull(NSTimeZone.timeZoneWithName("America/Los_Angeles"))
    formatter.dateFormat = pattern
    val date = NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())
    return formatter.stringFromDate(date)
}
