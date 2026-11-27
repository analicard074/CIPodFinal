package com.example.cipod_final.navigationBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.refresh
import com.example.cipod_final.data.CampusEvent
import com.example.cipod_final.ui.ScreenTitle
import com.example.cipod_final.ui.theme.ChannelBlue
import com.example.cipod_final.ui.theme.Seafoam
import com.example.cipod_final.ui.theme.WarmCream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun EventsScreen(loadEvents: suspend () -> List<CampusEvent>) {
    var events by remember { mutableStateOf(emptyList<CampusEvent>()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLoadError by remember { mutableStateOf(false) }
    var refreshCount by remember { mutableStateOf(0) }
    var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }

    // Reload when this tab opens or the student taps Refresh/Retry.
    LaunchedEffect(refreshCount) {
        isLoading = true
        hasLoadError = false
        try {
            events = withTimeout(20.seconds) { loadEvents() }
        } catch (_: TimeoutCancellationException) {
            hasLoadError = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            hasLoadError = true
        } finally {
            isLoading = false
        }
    }

    val selectedEvent = events.firstOrNull { event -> event.id == selectedEventId }
    Column(Modifier.fillMaxSize().background(WarmCream).padding(horizontal = 16.dp)) {
        EventsHeader(isLoading = isLoading, onRefresh = { refreshCount++ })
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            hasLoadError -> Column(Modifier.padding(16.dp)) {
                Text("Unable to load events. Please try again.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { refreshCount++ }) { Text("Retry") }
            }
            events.isEmpty() -> Text("No events are available yet.", Modifier.padding(vertical = 16.dp))
            else -> EventList(events, onSelectEvent = { event -> selectedEventId = event.id })
        }
    }

    if (!isLoading && !hasLoadError && selectedEvent != null) {
        EventDetailsDialog(selectedEvent, onDismiss = { selectedEventId = null })
    }
}

@Composable
private fun EventsHeader(isLoading: Boolean, onRefresh: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        ScreenTitle("Events", Modifier.padding(horizontal = 60.dp, vertical = 20.dp))
        FilledIconButton(
            onClick = onRefresh,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).shadow(3.dp, CircleShape),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Seafoam,
                contentColor = ChannelBlue,
            ),
        ) {
            Icon(
                painter = painterResource(Res.drawable.refresh),
                contentDescription = "Refresh events",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun EventList(events: List<CampusEvent>, onSelectEvent: (CampusEvent) -> Unit) {
    // Group by the full campus date, including the year, so different dates stay separate.
    val eventsByDay = remember(events) {
        events.groupBy { event -> formatEventDateTime(event.startAt.seconds, "yyyy-MM-dd") }
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 16.dp),
    ) {
        for ((date, dayEvents) in eventsByDay) {
            item(key = "date-$date") {
                Text(
                    formatEventDateTime(dayEvents.first().startAt.seconds, "EEEE, MMM d"),
                    modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 2.dp)
                        .semantics { heading() },
                    color = ChannelBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            items(dayEvents, key = { it.id }) { event ->
                EventCard(event, onClick = { onSelectEvent(event) })
            }
        }
    }
}

@Composable
private fun EventCard(event: CampusEvent, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Seafoam, contentColor = ChannelBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(formatEventDateTime(event.startAt.seconds, "h:mm a"), fontSize = 16.sp)
            Text(
                event.location,
                fontSize = 15.sp,
                color = ChannelBlue.copy(alpha = .85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EventDetailsDialog(event: CampusEvent, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title) },
        text = {
            Column(
                Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EventDetail("When", formatEventSchedule(event.startAt.seconds, event.endAt.seconds))
                EventDetail("Where", event.location)
                EventDetail("Description", event.description)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun EventDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
