package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.indoor.FloorPlanCatalog
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.indoor.FloorPlanRepository
import com.example.cipod_final.indoor.RoomMarkerType
import com.example.cipod_final.indoor.RoomDirectoryState
import com.example.cipod_final.indoor.rememberFloorPlanExporter
import kotlinx.coroutines.CancellationException

/** A full-screen modal preserves the outdoor map composition and its memory-only camera. */
@Composable
internal fun IndoorFloorViewer(buildingId: String, editor: FloorPlanEditorState, onBack: () -> Unit) {
    val plans = FloorPlanCatalog.forBuilding(buildingId)
    if (plans.isEmpty()) return
    var selectedId by remember(buildingId) { mutableStateOf(plans.first().id) }
    val plan = plans.first { it.id == selectedId }
    var selectedMarkerId by remember(plan.id) { mutableStateOf<String?>(null) }
    var pendingType by remember(plan.id) { mutableStateOf<RoomMarkerType?>(null) }
    var loadError by remember(plan.id) { mutableStateOf(false) }
    var loadAttempt by remember(plan.id) { mutableStateOf(0) }
    var showMarkers by remember { mutableStateOf(false) }
    var exportJson by remember { mutableStateOf<String?>(null) }
    var showDirectory by remember(buildingId) { mutableStateOf(false) }
    var directory by remember(buildingId) { mutableStateOf(RoomDirectoryState(buildingId)) }
    val markers = editor.markers(plan)
    val selected = markers.firstOrNull { it.id == selectedMarkerId }
    val exit = { editor.enableEditing(false); onBack() }
    LaunchedEffect(plan.id, loadAttempt) {
        if (!editor.loaded(plan)) {
            loadError = false
            try { editor.load(plan, FloorPlanRepository.load(plan)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { loadError = true }
        }
    }
    Dialog(onDismissRequest = exit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                val editorMaxHeight = (maxHeight * .30f).coerceAtMost(240.dp)
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Row {
                        TextButton(onClick = exit) { Text("Back to map") }
                        Text(CampusBuildings.find(buildingId)?.name.orEmpty(), Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleLarge)
                    }
                    if (plan.isDemonstration) {
                        Text("FICTIONAL DEMONSTRATION — not an actual building layout.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                        Text("Demonstration layouts are fictional test canvases, not verified floor plans or floor numbers.",
                            style = MaterialTheme.typography.bodySmall)
                    } else Text("Reviewed plan • ${plan.reviewedOn}", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = {
                            pendingType = null
                            selectedMarkerId = null
                            showDirectory = true
                        }) { Text("Room Directory") }
                        plans.forEach { item ->
                            TextButton(onClick = { selectedId = item.id }) {
                                Text(if (item.id == plan.id) "✓ ${item.label}" else item.label)
                            }
                        }
                        TextButton(onClick = { showMarkers = true }) { Text("Markers (${markers.size})") }
                    }
                    if (loadError) {
                        Text("Marker data could not be loaded. This plan's marker data needs review.")
                        TextButton(onClick = { loadAttempt++ }) { Text("Retry plan data") }
                    } else if (!editor.loaded(plan)) Text("Loading plan markers…")
                    if (editor.developerEnabled) {
                        Column(Modifier.fillMaxWidth().heightIn(max = editorMaxHeight).verticalScroll(rememberScrollState())) {
                            Text("DEVELOPER TOOLS • ${if (editor.dirty(plan)) "Unsaved draft in memory" else "Session draft"}",
                                style = MaterialTheme.typography.labelMedium)
                            Row {
                                TextButton(onClick = {
                                    editor.enableEditing(!editor.editing)
                                    pendingType = null
                                    selectedMarkerId = null
                                }, enabled = editor.loaded(plan)) { Text(if (editor.editing) "Read-only preview" else "Edit markers") }
                                TextButton(onClick = { exportJson = editor.export(plan) }, enabled = editor.loaded(plan)) { Text("Export JSON") }
                            }
                            FloorPlanEditorControls(plan, editor, selected, pendingType,
                                onArmPlacement = { pendingType = it; selectedMarkerId = null },
                                onDelete = { selectedMarkerId = null })
                        }
                    }
                    key(plan.id) {
                        FloorPlanCanvas(
                            plan = plan, markers = markers, editing = editor.developerEnabled && editor.editing,
                            selectedMarkerId = selectedMarkerId,
                            onSelectMarker = { selectedMarkerId = it; pendingType = null },
                            onPlaceMarker = { point ->
                                pendingType?.let { type ->
                                    editor.add(plan, type, point)?.let { selectedMarkerId = it.id }
                                    pendingType = null
                                }
                            },
                            onMoveMarker = { id, point -> editor.move(plan, id, point) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    if (showDirectory) {
        RoomDirectoryPanel(directory, onChange = { directory = it }, onBack = { showDirectory = false })
    }
    if (showMarkers) {
        AlertDialog(onDismissRequest = { showMarkers = false }, title = { Text("${plan.label} markers") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (markers.isEmpty()) Text("No markers on this layout.")
                    markers.forEach { marker ->
                        TextButton(onClick = { selectedMarkerId = marker.id; pendingType = null; showMarkers = false }) {
                            Text("${marker.name} • ${marker.type.label}")
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { showMarkers = false }) { Text("Close") } })
    }
    if (selected != null && !editor.editing && !showMarkers) {
        AlertDialog(onDismissRequest = { selectedMarkerId = null }, title = { Text(selected.name) },
            text = { Text("${selected.type.label}${if (plan.isDemonstration) " • fictional demonstration marker" else ""}") },
            confirmButton = { TextButton(onClick = { selectedMarkerId = null }) { Text("Close") } })
    }
    exportJson?.let { json ->
        // Clipboard is accessed only by this explicit developer export action.
        val copyJson = rememberFloorPlanExporter()
        var copyResult by remember(json) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { exportJson = null }, title = { Text("Export ${plan.id}.json") },
            text = {
                Column {
                    Text("Copy this JSON and save it in your project. This does not save the draft automatically.")
                    OutlinedTextField(value = json, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp), minLines = 5)
                    copyResult?.let { Text(it) }
                }
            }, confirmButton = { TextButton(onClick = {
                copyResult = if (copyJson(json)) "Copied JSON. Save it to a file to keep your edits."
                    else "Clipboard unavailable. Select and copy the JSON text manually."
            }) { Text("Copy JSON") } },
            dismissButton = { TextButton(onClick = { exportJson = null }) { Text("Close") } })
    }
}
