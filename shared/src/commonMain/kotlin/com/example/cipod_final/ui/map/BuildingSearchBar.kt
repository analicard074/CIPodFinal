package com.example.cipod_final.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.cipod_final.buildings.CampusBuilding
import com.example.cipod_final.ui.ScreenTitle

@Composable
internal fun mapSearchFieldColor(): Color = lerp(
    ButtonDefaults.elevatedButtonColors().containerColor,
    Color.White,
    0.45f,
)

@Composable
internal fun BuildingSearchBar(
    query: String,
    results: List<CampusBuilding>,
    onQueryChange: (String) -> Unit,
    onBuildingClick: (CampusBuilding) -> Unit,
    maxResultsHeight: Dp = 144.dp,
    onResultsVisibilityChange: (Boolean) -> Unit = {}
) {
    var resultsExpanded by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                .heightIn(min = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScreenTitle("Resources")
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            val searchFieldColor = mapSearchFieldColor()
            val showResults = query.isNotBlank() && resultsExpanded && fieldSize.width > 0
            LaunchedEffect(showResults) {
                onResultsVisibilityChange(showResults)
            }
            val searchFieldShape = if (showResults) {
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            } else {
                RoundedCornerShape(24.dp)
            }
            val outlineWidth = if (fieldFocused) 2.dp else 1.dp
            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        resultsExpanded = true
                        onQueryChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { fieldSize = it }
                        .onFocusChanged {
                            fieldFocused = it.isFocused
                            if (it.isFocused) resultsExpanded = true
                        }
                        .background(searchFieldColor, searchFieldShape),
                    label = { Text("Search campus resources") },
                    placeholder = { Text("Try 'Financial Aid'") },
                    singleLine = true,
                    shape = searchFieldShape,
                    trailingIcon = {
                        if (query.isNotEmpty()) TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
                    }
                )
                if (showResults) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(
                            0,
                            fieldSize.height - with(density) { outlineWidth.roundToPx() }),
                        onDismissRequest = { resultsExpanded = false },
                        properties = PopupProperties(focusable = false),
                    ) {
                        val resultsShape =
                            RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        LazyColumn(
                            modifier = Modifier
                                .width(with(density) { fieldSize.width.toDp() })
                                .heightIn(max = maxResultsHeight)
                                .clip(resultsShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    outlineWidth,
                                    if (fieldFocused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    resultsShape,
                                ),
                        ) {
                            if (results.isEmpty()) {
                                item {
                                    Text(
                                        "No buildings found. Try another name.",
                                        Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            items(results, key = { it.id }) { building ->
                                Column(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        .clickable {
                                            resultsExpanded = false
                                            onBuildingClick(building)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        building.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
