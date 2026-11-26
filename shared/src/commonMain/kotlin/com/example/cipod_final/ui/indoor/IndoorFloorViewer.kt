package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.indoor.BroomeIndoorRoutes
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.indoor.FloorPlanRepository
import com.example.cipod_final.indoor.IndoorConnection
import com.example.cipod_final.indoor.RoomDirectory
import com.example.cipod_final.indoor.RoomHighlightRegistry
import com.example.cipod_final.indoor.RoomMarkerType
import com.example.cipod_final.indoor.rememberFloorPlanExporter
import kotlinx.coroutines.CancellationException

/** Fills the Directions content area while the app's bottom navigation stays visible. */
@Composable
internal fun IndoorFloorViewer(
    buildingId: String,
    editor: FloorPlanEditorState,
    onBack: () -> Unit,
    initialRoomId: String? = null,
    routingToRoom: Boolean = false,
    accessibilityEnabled: Boolean = false,
    startRoomId: String? = null,
    showOutdoorBackLink: Boolean = false,
) {
    val plans = FloorPlanCatalog.forBuilding(buildingId)
    if (plans.isEmpty()) return
    val initialRoom = RoomDirectory.find(buildingId, initialRoomId)
    val startRoom = RoomDirectory.find(buildingId, startRoomId)
    val graph = remember(buildingId) { BroomeIndoorRoutes.current()?.takeIf { it.buildingId == buildingId } }
    var elevatorSelected by remember(buildingId, initialRoomId, startRoomId, accessibilityEnabled) { mutableStateOf(false) }
    val useElevator = accessibilityEnabled || elevatorSelected
    val route = remember(graph, initialRoomId, startRoomId, routingToRoom, useElevator) {
        when {
            !routingToRoom || initialRoomId == null -> null
            startRoomId != null -> graph?.routeFromRoom(
                startRoomId, initialRoomId, avoidStairs = useElevator, avoidElevators = !useElevator)
            else -> graph?.routeFromOutside(
                initialRoomId, avoidStairs = useElevator, avoidElevators = !useElevator)
        }
    }
    val startingFloor = startRoom?.floorPlanId
        ?: graph?.nodes?.firstOrNull { it.id == graph.mainEntrance?.nodeId }?.floorPlanId
    val showElevatorChoice = routingToRoom && !accessibilityEnabled && initialRoom != null &&
        initialRoom.floorPlanId != startingFloor
    var selectedId by remember(buildingId, initialRoomId, startRoomId, routingToRoom) {
        mutableStateOf(route?.nodes?.firstOrNull()?.floorPlanId
            ?: initialRoom?.floorPlanId?.takeIf { id -> plans.any { it.id == id } } ?: plans.first().id)
    }
    val plan = plans.first { it.id == selectedId }
    var selectedMarkerId by remember(plan.id) { mutableStateOf<String?>(null) }
    var pendingType by remember(plan.id) { mutableStateOf<RoomMarkerType?>(null) }
    var loadError by remember(plan.id) { mutableStateOf(false) }
    var loadAttempt by remember(plan.id) { mutableStateOf(0) }
    var showMarkers by remember { mutableStateOf(false) }
    var exportJson by remember { mutableStateOf<String?>(null) }
    val markers = editor.markers(plan)
    val selected = markers.firstOrNull { it.id == selectedMarkerId }
    val exit = {
        editor.enableEditing(false)
        onBack()
    }
    DisposableEffect(editor) {
        onDispose { editor.enableEditing(false) }
    }
    LaunchedEffect(plan.id, loadAttempt) {
        if (!editor.loaded(plan)) {
            loadError = false
            try {
                editor.load(plan, FloorPlanRepository.load(plan))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadError = true
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                val editorMaxHeight = (maxHeight * .20f).coerceAtMost(180.dp)
                val headerMaxHeight = (maxHeight * .25f).coerceAtMost(160.dp)
                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = headerMaxHeight)
                            .padding(horizontal = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                CampusBuildings.find(buildingId)?.name.orEmpty(),
                                Modifier.fillMaxWidth().padding(horizontal = 72.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(onClick = exit, modifier = Modifier.align(Alignment.CenterStart)) { Text("Back") }
                        }
                        if (plan.isDemonstration) {
                            Text(
                                "FICTIONAL DEMONSTRATION — not an actual building layout.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Demonstration layouts are fictional test canvases, not verified floor plans or floor numbers.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            plans.forEach { item ->
                                TextButton(onClick = { selectedId = item.id }) {
                                    val floorLabel = item.label.substringBefore(" •")
                                    Text(if (item.id == plan.id) "✓ $floorLabel" else floorLabel)
                                }
                            }
                            if (showElevatorChoice) {
                                TextButton(onClick = { elevatorSelected = !elevatorSelected }) {
                                    Text(if (useElevator) "Use stairs" else "Use elevator")
                                }
                            }
                            if (markers.isNotEmpty()) {
                                TextButton(onClick = {
                                    showMarkers = true
                                }) { Text("Markers (${markers.size})") }
                            }
                        }
                        if (routingToRoom && initialRoom != null && graph != null) {
                            Text(startRoom?.let { "Room ${it.number} → Room ${initialRoom.number}" }
                                ?: "To room ${initialRoom.number} • Enter through the Main Entrance.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            if (route == null) {
                                val connection = if (useElevator) "elevator" else "stairs"
                                val origin = if (startRoom == null) "the Main Entrance" else "room ${startRoom.number}"
                                Text("No connected $connection route from $origin.", textAlign = TextAlign.Center)
                            }
                            if (route != null) {
                                val floorChange = route.edges.firstOrNull { it.connection != IndoorConnection.Walk }
                                val connectorLabel = floorChange?.let { edge ->
                                    graph.nodes.first { it.id == edge.from }.label.substringBefore(",")
                                        .replace("Main entrance", "Main Entrance")
                                }
                                Text("Blue: route • Orange: room door",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center)
                                if (floorChange != null && connectorLabel != null) {
                                    val arrivalPlanId = route.nodes[route.edges.indexOf(floorChange) + 1].floorPlanId
                                    val arrivalFloor = plans.first { it.id == arrivalPlanId }
                                        .label.substringBefore(" •").lowercase()
                                    Text("Use $connectorLabel to go to the $arrivalFloor.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                        if (loadError) {
                            Text("Marker data could not be loaded. This plan's marker data needs review.", textAlign = TextAlign.Center)
                            TextButton(onClick = { loadAttempt++ }) { Text("Retry plan data") }
                        } else if (!editor.loaded(plan)) Text("Loading plan markers…")
                    }
                    if (editor.developerEnabled) {
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = editorMaxHeight)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "DEVELOPER TOOLS • ${if (editor.dirty(plan)) "Unsaved draft in memory" else "Session draft"}",
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        editor.enableEditing(!editor.editing)
                                        pendingType = null
                                        selectedMarkerId = null
                                    },
                                    enabled = editor.loaded(plan)
                                ) { Text(if (editor.editing) "Read-only preview" else "Edit markers") }
                                TextButton(
                                    onClick = { exportJson = editor.export(plan) },
                                    enabled = editor.loaded(plan)
                                ) { Text("Export JSON") }
                            }
                            FloorPlanEditorControls(
                                plan, editor, selected, pendingType,
                                onArmPlacement = {
                                    pendingType = it
                                    selectedMarkerId = null
                                },
                                onDelete = { selectedMarkerId = null })
                        }
                    }
                    key(plan.id, useElevator, startRoomId) {
                        val selectedRoom = initialRoom

                        val roomHighlight = selectedRoom?.let { room ->
                            RoomHighlightRegistry.find(
                                roomId = room.id,
                                floorPlanId = plan.id,
                                planRevision = plan.revision
                            )
                        }

                        FloorPlanCanvas(
                            plan = plan,
                            markers = markers,
                            editing = editor.developerEnabled && editor.editing,
                            selectedMarkerId = selectedMarkerId,

                            roomHighlight = roomHighlight,
                            highlightColor = selectedRoom?.category?.color,
                            route = route,

                            onSelectMarker = {
                                selectedMarkerId = it
                                pendingType = null
                            },

                            onPlaceMarker = { point ->
                                pendingType?.let { type ->
                                    editor.add(plan, type, point)?.let {
                                        selectedMarkerId = it.id
                                    }
                                    pendingType = null
                                }
                            },

                            onMoveMarker = { id, point ->
                                editor.move(plan, id, point)
                            },

                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showOutdoorBackLink) {
                        TextButton(onClick = exit, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Back to outdoor map")
                        }
                    }
                }
            }
        }

        if (showMarkers) {
            AlertDialog(
                onDismissRequest = { showMarkers = false },
                title = { Text("${plan.label} markers") },
                text = {
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        if (markers.isEmpty()) Text("No markers on this layout.")
                        markers.forEach { marker ->
                            TextButton(onClick = {
                                selectedMarkerId = marker.id
                                pendingType = null
                                showMarkers = false
                            }) {
                                Text("${marker.name} • ${marker.type.label}")
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showMarkers = false }) { Text("Close") } })
        }
        if (selected != null && !editor.editing && !showMarkers) {
            AlertDialog(
                onDismissRequest = { selectedMarkerId = null },
                title = { Text(selected.name) },
                text = {
                    Text(
                        "${selected.type.label}${
                            when {
                                plan.isDemonstration -> " • fictional demonstration marker"
                                plan.isReferenceDraft -> " • unverified draft marker"
                                else -> ""
                            }
                        }"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedMarkerId = null
                    }) { Text("Close") }
                })
        }
        exportJson?.let { json ->
            // Clipboard is accessed only by this explicit developer export action.
            val copyJson = rememberFloorPlanExporter()
            var copyResult by remember(json) { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { exportJson = null },
                title = { Text("Export ${plan.id}.json") },
                text = {
                    Column {
                        Text("Copy this JSON and save it in your project. This does not save the draft automatically.")
                        OutlinedTextField(
                            value = json, onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp), minLines = 5
                        )
                        copyResult?.let { Text(it) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        copyResult =
                            if (copyJson(json)) "Copied JSON. Save it to a file to keep your edits."
                            else "Clipboard unavailable. Select and copy the JSON text manually."
                    }) { Text("Copy JSON") }
                },
                dismissButton = { TextButton(onClick = { exportJson = null }) { Text("Close") } })
        }
    }
}
