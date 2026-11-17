package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cipod_final.indoor.FloorPlan
import com.example.cipod_final.indoor.FloorPlanEditorState
import com.example.cipod_final.indoor.RoomMarker
import com.example.cipod_final.indoor.RoomMarkerType

@Composable
internal fun FloorPlanEditorControls(
    plan: FloorPlan,
    editor: FloorPlanEditorState,
    selected: RoomMarker?,
    pendingType: RoomMarkerType?,
    onArmPlacement: (RoomMarkerType) -> Unit,
    onDelete: () -> Unit
) {
    if (!editor.developerEnabled || !editor.editing) return
    Column {
        Text(if (pendingType == null) "Choose an Add type, then tap the plan. Drag a marker to move it."
            else "Tap the plan to add a ${pendingType.label.lowercase()}. Pinch to zoom; drag blank space to pan.")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            RoomMarkerType.entries.forEach { type ->
                TextButton(onClick = { onArmPlacement(type) }) {
                    Text(if (pendingType == type) "✓ Add ${type.label}" else "Add ${type.label}")
                }
            }
        }
        if (selected != null) {
            var name by remember(selected.id, selected.name) { mutableStateOf(selected.name) }
            var type by remember(selected.id, selected.type) { mutableStateOf(selected.type) }
            OutlinedTextField(
                value = name, onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Marker name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = name.isBlank()
            )
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                RoomMarkerType.entries.forEach { option ->
                    TextButton(onClick = { type = option }) {
                        Text(if (type == option) "✓ ${option.label}" else option.label)
                    }
                }
            }
            Row {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    editor.update(plan, selected.id, name, type)
                }) { Text("Apply name and type") }
                TextButton(onClick = {
                    editor.delete(plan, selected.id)
                    onDelete()
                }) { Text("Delete marker") }
            }
        }
    }
}
