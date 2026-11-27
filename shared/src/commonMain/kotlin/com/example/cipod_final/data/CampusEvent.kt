package com.example.cipod_final.data

import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.toDuration
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// Field names and timestamp types match the events collection in Firestore.
@Serializable
data class CampusEvent(
    val title: String,
    val description: String,
    val location: String,
    val startAt: Timestamp,
    val endAt: Timestamp,
    // Firestore supplies the document ID separately from the event's fields.
    @Transient val id: String = "",
)

internal fun prepareEvents(events: List<CampusEvent>): List<CampusEvent> {
    for (event in events) {
        val hasDetails = event.title.isNotBlank() &&
            event.description.isNotBlank() && event.location.isNotBlank()
        require(hasDetails) {
            "Event ${event.id} needs a title, description, and location."
        }
        require(event.endAt.toDuration() >= event.startAt.toDuration()) {
            "Event ${event.id} must end at or after its start time."
        }
    }
    // Sort by start time, then use the title and ID to break ties consistently.
    return events.sortedWith(
        compareBy<CampusEvent> { it.startAt.seconds }
            .thenBy { it.startAt.nanoseconds }
            .thenBy { it.title }
            .thenBy { it.id }
    )
}
