package com.example.cipod_final

import com.example.cipod_final.data.CampusEvent
import com.example.cipod_final.data.prepareEvents
import com.example.cipod_final.navigationBar.formatEventDateTime
import com.example.cipod_final.navigationBar.formatEventSchedule
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class CampusEventTest {
    private val event = CampusEvent(
        title = "Scoop There It Is!",
        description = "Sip, Scoop & Study Break at the Student Union.",
        location = "Student Union Dolphin Deck, Student Union Seabreeze Lawn",
        startAt = Timestamp(utcSeconds("2026-12-02T20:00:00Z"), 0),
        endAt = Timestamp(utcSeconds("2026-12-02T22:00:00Z"), 0),
        id = "event-one",
    )

    @Test fun ordersEventsByTimeWithoutLosingTheirDetailsOrDocumentIds() {
        val earlier = event.copy(id = "earlier", startAt = Timestamp(event.startAt.seconds - 60, 0))
        val later = event.copy(id = "later", startAt = Timestamp(event.startAt.seconds, 1))
        assertEquals(listOf(earlier, event, later), prepareEvents(listOf(later, event, earlier)))
        assertEquals(emptyList(), prepareEvents(emptyList()))
    }

    @Test fun rejectsMissingDetailsAndEndTimesBeforeTheStart() {
        val invalidEvents = listOf(
            event.copy(title = " "),
            event.copy(description = ""),
            event.copy(location = ""),
            event.copy(endAt = Timestamp(event.startAt.seconds - 1, 0)),
            event.copy(startAt = Timestamp(event.endAt.seconds, 1)),
        )
        for (invalid in invalidEvents) {
            assertFailsWith<IllegalArgumentException> { prepareEvents(listOf(invalid)) }
        }
    }

    @Test fun screenshotEventDisplaysInCampusTime() {
        assertEquals("Wed, Dec 2, 2026 · 12:00 PM PST", formatEventDateTime(event.startAt.seconds))
        assertEquals("Wed, Dec 2, 2026 · 2:00 PM PST", formatEventDateTime(event.endAt.seconds))
    }

    @Test fun campusTimeHandlesDaylightSavingTimeAndDatesAcrossMidnight() {
        assertEquals("Thu, Jul 2, 2026 · 12:00 PM PDT", formatEventDateTime(utcSeconds("2026-07-02T19:00:00Z")))
        assertEquals("Wed, Dec 2, 2026 · 11:30 PM PST", formatEventDateTime(utcSeconds("2026-12-03T07:30:00Z")))
        assertEquals("Thu, Dec 3, 2026 · 12:30 AM PST", formatEventDateTime(utcSeconds("2026-12-03T08:30:00Z")))
    }

    @Test fun listHeadingsAndTimesUseTheCampusDateInsteadOfTheUtcDate() {
        assertEquals("Wednesday, Dec 2", formatEventDateTime(event.startAt.seconds, "EEEE, MMM d"))
        assertEquals("12:00 PM", formatEventDateTime(event.startAt.seconds, "h:mm a"))
        assertEquals("2026-12-02", formatEventDateTime(utcSeconds("2026-12-03T07:30:00Z"), "yyyy-MM-dd"))
        assertEquals("2026-12-03", formatEventDateTime(utcSeconds("2026-12-03T08:30:00Z"), "yyyy-MM-dd"))
    }

    @Test fun multiDayScheduleSeparatesTheDateRangeFromTheTimeRange() {
        assertEquals(
            "Wed, Nov 11, 2026 - Fri, Feb 26, 2027\n8:00 AM - 9:00 PM PST",
            formatEventSchedule(utcSeconds("2026-11-11T16:00:00Z"), utcSeconds("2027-02-27T05:00:00Z")),
        )
    }

    @Test fun sameDayScheduleShowsTheDateOnce() {
        assertEquals(
            "Wed, Dec 2, 2026\n12:00 PM - 2:00 PM PST",
            formatEventSchedule(event.startAt.seconds, event.endAt.seconds),
        )
    }

    @Test fun scheduleShowsBothTimezonesAcrossDaylightSavingChanges() {
        assertEquals(
            "Sat, Oct 31, 2026 - Sun, Nov 1, 2026\n10:00 PM PDT - 2:00 AM PST",
            formatEventSchedule(utcSeconds("2026-11-01T05:00:00Z"), utcSeconds("2026-11-01T10:00:00Z")),
        )
    }

    // A Z at the end of a date means UTC; the screen converts it to campus Pacific time.
    private fun utcSeconds(dateTime: String): Long {
        return Instant.parse(dateTime).epochSeconds
    }

}
