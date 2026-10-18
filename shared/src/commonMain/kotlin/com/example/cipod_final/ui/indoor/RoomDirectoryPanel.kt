package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.RoomDirectoryState
import com.example.cipod_final.indoor.RoomLocationRegistry

/** Separate modal retains the mounted floor canvas, its dimensions, zoom, pan and editor drafts. */
@Composable
internal fun RoomDirectoryPanel(
    state: RoomDirectoryState,
    onChange: (RoomDirectoryState) -> Unit,
    onBack: () -> Unit
) {
    val scroll = rememberLazyListState()
    LaunchedEffect(state.selectedRoomId) {
        if (state.selectedRoomId != null) scroll.scrollToItem(0)
    }
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)) {
                TextButton(onClick = onBack) { Text("Back to floor viewer") }
                Text("${CampusBuildings.find(state.buildingId)?.name.orEmpty()} room directory",
                    style = MaterialTheme.typography.titleLarge)
                Text("Published room information. Indoor positions and floor assignments are not verified.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.query, onValueChange = { onChange(state.copy(query = it)) },
                    label = { Text("Search room number or type") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    trailingIcon = {
                        if (state.query.isNotEmpty()) TextButton(onClick = { onChange(state.copy(query = "")) }) { Text("Clear") }
                    }
                )
                LazyColumn(Modifier.weight(1f), state = scroll, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.selectedRoom?.let { room ->
                        item(key = "selected-room") {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Room ${room.number}", style = MaterialTheme.typography.titleMedium)
                                    Text("Published type: ${room.publishedType}")
                                    val location = RoomLocationRegistry.current.find(room, FloorPlanCatalog.plans)
                                    Text(if (location == null) RoomLocationRegistry.pendingMessage
                                        else "Verified position on plan ${location.floorPlanId}")
                                    Text("Source: CSUCI Classrooms and Labs • Reviewed ${room.reviewedOn}",
                                        style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { onChange(state.clearSelection()) }) { Text("Close room details") }
                                }
                            }
                        }
                    }
                    item { Text("${state.results.size} rooms") }
                    if (state.results.isEmpty()) item { Text("No matching rooms.") }
                    items(state.results, key = { it.id }) { room ->
                        OutlinedButton(onClick = { onChange(state.select(room.id)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${room.number} • ${room.publishedType}")
                        }
                    }
                }
            }
        }
    }
}
