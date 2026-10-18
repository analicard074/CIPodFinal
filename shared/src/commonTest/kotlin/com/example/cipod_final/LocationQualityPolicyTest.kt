package com.example.cipod_final

import com.example.cipod_final.location.LocationQuality
import com.example.cipod_final.location.LocationQualityPolicy
import com.example.cipod_final.location.LocationReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LocationQualityPolicyTest {
    private val now = 1_800_000_000_000L
    private val fix = LocationReading(34.162197, -119.044662, 10.0, now, true)

    @Test fun acceptsFreshPreciseFixAtAccuracyTarget() {
        assertEquals(LocationQuality.Usable, LocationQualityPolicy.evaluate(fix.copy(accuracyMeters = 25.0), now))
    }
    @Test fun poorerAccuracyIsApproximate() {
        assertEquals(LocationQuality.Approximate, LocationQualityPolicy.evaluate(fix.copy(accuracyMeters = 25.1), now))
    }
    @Test fun reducedPermissionIsApproximateEvenWithSmallReportedRadius() {
        assertEquals(LocationQuality.Approximate, LocationQualityPolicy.evaluate(fix.copy(precisePermission = false), now))
    }
    @Test fun freshnessHasAnExplicitBoundary() {
        assertEquals(LocationQuality.Usable, LocationQualityPolicy.evaluate(fix, now + 29_999))
        assertEquals(LocationQuality.Stale, LocationQualityPolicy.evaluate(fix, now + 30_000))
    }
    @Test fun staleFixCannotBecomeCurrent() {
        assertEquals(LocationQuality.Stale, LocationQualityPolicy.evaluate(fix.copy(capturedAtMillis = now - 60_000), now))
    }
    @Test fun implausiblyFutureTimestampIsRejected() {
        assertEquals(LocationQuality.Stale, LocationQualityPolicy.evaluate(fix.copy(capturedAtMillis = now + 6_000), now))
    }
    @Test fun rejectsMissingOrInvalidAccuracy() {
        listOf(-1.0, 0.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertEquals(LocationQuality.Invalid, LocationQualityPolicy.evaluate(fix.copy(accuracyMeters = it), now))
        }
    }
    @Test fun rejectsInvalidCoordinates() {
        listOf(fix.copy(latitude = 91.0), fix.copy(longitude = -181.0),
            fix.copy(latitude = Double.NaN), fix.copy(longitude = Double.POSITIVE_INFINITY)).forEach {
            assertEquals(LocationQuality.Invalid, LocationQualityPolicy.evaluate(it, now))
        }
    }
    @Test fun rejectsMissingTimestamp() {
        assertEquals(LocationQuality.Invalid, LocationQualityPolicy.evaluate(fix.copy(capturedAtMillis = 0), now))
    }
    @Test fun debugStringDoesNotRevealCoordinates() {
        assertFalse(fix.toString().contains("34.162197"))
        assertFalse(fix.toString().contains("-119.044662"))
    }
}
