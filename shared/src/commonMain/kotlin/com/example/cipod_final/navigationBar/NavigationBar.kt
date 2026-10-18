package com.example.cipod_final.navigationBar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.location.LocationSession
import com.example.cipod_final.indoor.FloorPlanEditorViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cipod_final.ui.map.CampusMapScreen

private val CIBlue = Color(0xFF174A5D)

@Composable
fun NaviBar(modifier: Modifier = Modifier, indoorEditorEnabled: Boolean = false) {
    val indoorModel: FloorPlanEditorViewModel = viewModel(key = "indoor-editor-$indoorEditorEnabled") {
        FloorPlanEditorViewModel(indoorEditorEnabled)
    }
    val floorEditor = indoorModel.editor
    var selected by remember { mutableStateOf("MyPod") }
    val locationScope = rememberCoroutineScope()
    // Device coordinates must never enter the saved navigation state below.
    val locationSession = remember(locationScope) { LocationSession(locationScope) }

    // Keep campus search, building preview, and destination alive across tab changes.
    var navigation by rememberSaveable(stateSaver = listSaver<CampusNavigationState, String>(
        save = { listOf(it.query, it.selectedBuildingId.orEmpty(), it.destinationId.orEmpty()) },
        restore = { CampusNavigationState(it[0], it[1].ifEmpty { null }, it[2].ifEmpty { null }) }
    )) { mutableStateOf(CampusNavigationState()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationButtons(
                selected = selected,
                onSectionSelected = { selected = it }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (selected) {
                "Directions" -> CampusMapScreen(
                    navigation = navigation,
                    locationSession = locationSession,
                    floorEditor = floorEditor,
                    onNavigationChange = { navigation = it }
                )
                "MyPod" -> MyPodScreen()
                else -> Text(
                    text = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    color = Color.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NavigationButtons(
    selected: String,
    onSectionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = CIBlue,
        contentColor = Color.White
    ) {
        NaviButton("Events", selected, onSectionSelected)
        NaviButton("Directions", selected, onSectionSelected)
        NaviButton("MyPod", selected, onSectionSelected)
        NaviButton("Resources", selected, onSectionSelected)
        NaviButton("Settings", selected, onSectionSelected)
    }
}

@Composable
private fun RowScope.NaviButton(
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == label

    TextButton(
        onClick = { onSelect(label) },
        modifier = Modifier.weight(1f),
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = if (isSelected) CIBlue else Color.White
        ),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = if (isSelected) 14.sp else 12.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
