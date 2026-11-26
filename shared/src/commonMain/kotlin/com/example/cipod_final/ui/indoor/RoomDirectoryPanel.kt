package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.indoor.BroomeIndoorRoutes
import com.example.cipod_final.indoor.DirectoryRoom
import com.example.cipod_final.indoor.RoomDirectory
import com.example.cipod_final.indoor.RoomDirectoryState
import com.example.cipod_final.ui.ScreenTitle

/** Separate modal retains the mounted floor canvas, its dimensions, zoom, pan and editor drafts. */
@Composable
internal fun RoomDirectoryPanel(
    state: RoomDirectoryState,
    onChange: (RoomDirectoryState) -> Unit,
    onBack: () -> Unit,
    backLabel: String = "Back to floor viewer",
    onViewFloorPlan: (() -> Unit)? = null,
    onDirectionsToRoom: ((DirectoryRoom) -> Unit)? = null,
    onStartFromRoom: ((DirectoryRoom) -> Unit)? = null,
    startRoomId: String? = null,
) {
    val scroll = rememberLazyListState()
    var expandedGroup by rememberSaveable(state.buildingId) { mutableStateOf<String?>(null) }
    val groups = state.results.groupBy { it.category?.name ?: it.publishedType }
    LaunchedEffect(state.selectedRoomId) {
        if (state.selectedRoomId != null) scroll.scrollToItem(0)
    }
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text(backLabel) }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { onViewFloorPlan?.invoke() },
                        enabled = onViewFloorPlan != null
                    ) { Text("View Floor Plan") }
                }
                ScreenTitle(CampusBuildings.find(state.buildingId)?.name.orEmpty())
                Text(
                    "Room Directory",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = state.query, onValueChange = { onChange(state.copy(query = it)) },
                    label = { Text("Search room number or type") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            TextButton(onClick = { onChange(state.copy(query = "")) }) { Text("Clear") }
                        }
                    }
                )
                if (startRoomId != null) {
                    val start = RoomDirectory.find(state.buildingId, startRoomId)
                    if (start != null) {
                        Text("Starting point: Room ${start.number} • Choose a destination",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
                LazyColumn(
                    Modifier.weight(1f),
                    state = scroll,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.selectedRoom?.let { room ->
                        item(key = "selected-room") {
                            Card(
                                Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        room.category?.let { category ->
                                            Box(Modifier.padding(end = 12.dp).size(18.dp)
                                                .background(category.color, CircleShape))
                                        }
                                        Text("Room ${room.number}",
                                            style = MaterialTheme.typography.titleMedium)
                                    }

                                    RoomNavigationActions(room, onDirectionsToRoom, onStartFromRoom, startRoomId)
                                }
                            }
                        }
                    }
                    item { Text("${state.results.size} rooms") }
                    if (state.results.isEmpty()) item { Text("No matching rooms.") }

                    groups.forEach { (groupId, rooms) ->
                        val category = rooms.first().category
                        val expanded = expandedGroup == groupId
                        item(key = "group-$groupId") {
                            OutlinedButton(
                                onClick = { expandedGroup = if (expanded) null else groupId },
                                modifier = Modifier.fillMaxWidth().semantics {
                                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                                },
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    category?.let {
                                        Box(Modifier.padding(end = 12.dp).size(16.dp)
                                            .background(it.color, CircleShape))
                                    }
                                    Text("${category?.label ?: rooms.first().publishedType} (${rooms.size})",
                                        modifier = Modifier.weight(1f))
                                    Text(if (expanded) "−" else "+")
                                }
                            }
                        }
                        if (expanded) {
                            items(rooms, key = { it.id }) { room ->
                                OutlinedButton(
                                    onClick = { onChange(state.select(room.id)) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Show a colored circle for categorized rooms.
                                        room.category?.let { category ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .size(16.dp)
                                                    .background(
                                                        color = category.color,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }

                                        Text("${room.number} • ${room.publishedType}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomNavigationActions(
    room: DirectoryRoom,
    onDirections: ((DirectoryRoom) -> Unit)?,
    onStart: ((DirectoryRoom) -> Unit)?,
    startRoomId: String?,
) {
    if (onDirections == null && onStart == null) return
    val available = BroomeIndoorRoutes.current()?.supports(room) == true
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onStart != null) OutlinedButton(onClick = { onStart(room) }, enabled = available,
            modifier = Modifier.weight(1f)) { Text(if (startRoomId == room.id) "Starting Here" else "Start Here") }
        if (onDirections != null) Button(onClick = { onDirections(room) }, enabled = available,
            modifier = Modifier.weight(1f)) { Text("Directions") }
    }
    if (!available) Text("Indoor route unavailable: doorway or hallway connection needs review.",
        style = MaterialTheme.typography.bodySmall)
}
