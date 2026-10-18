package com.example.cipod_final.navigationBar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.accessibility
import cipodfinal.shared.generated.resources.placeholder_map
import org.jetbrains.compose.resources.painterResource

data class MyPodClass(
    val name: String,
    val meetingTime: String,
    val location: String
)

@Composable
fun MyPodScreen(
    modifier: Modifier = Modifier,
    accessibilityEnabled: Boolean,
    onAccessibilityChange: (Boolean) -> Unit,
    classes: List<MyPodClass>,
    onClassesChange: (List<MyPodClass>) -> Unit,
    onDirectionsClick: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }

    // list of buildings (add more later)
    val buildings = listOf("Sierra Hall", "Gateway Hall")

    // check which buildings match the search
    val results = buildings.filter { building ->
        building.contains(search.trim(), ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to MyPod",
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Switch(
                checked = accessibilityEnabled,
                onCheckedChange = onAccessibilityChange,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .semantics { contentDescription = "Accessibility" },
                thumbContent = {
                    Icon(
                        painter = painterResource(Res.drawable.accessibility),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Image(
                painter = painterResource(Res.drawable.placeholder_map),
                contentDescription = "Campus map",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            ) {
                // the search box
                OutlinedTextField(
                    value = search,
                    onValueChange = { newText ->
                        search = newText
                        picked = null
                    },
                    label = { Text("Search") },
                    placeholder = { Text("Example: Sierra Hall") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = {
                                search = ""
                                picked = null
                            }) {
                                Text("x")
                            }
                        }
                    }
                )

                // if user picked a building, display it
                if (picked != null) {
                    Text("$picked selected.", Modifier.padding(16.dp))
                } else if (search.isNotBlank()) {
                    // lists matches
                    if (results.isEmpty()) {
                        Text("Not found.", Modifier.padding(16.dp))
                    } else {
                        for (building in results) {
                            TextButton(onClick = {
                                search = building
                                picked = building
                            }) {
                                Text(building)
                            }
                        }
                    }
                }

                ClassesSection(
                    classes = classes,
                    onClassesChange = onClassesChange,
                    onDirectionsClick = onDirectionsClick
                )
            }
        }
    }
}

@Composable
private fun ClassesSection(
    classes: List<MyPodClass>,
    onClassesChange: (List<MyPodClass>) -> Unit,
    onDirectionsClick: () -> Unit
) {
    var showAddClass by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var meetingTime by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var pendingRemoval by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Classes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            TextButton(
                onClick = { showAddClass = true }
            ) {
                Text("Add class")
            }
        }
        if (classes.isEmpty()) {
            Text("No classes added yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                classes.forEachIndexed { index, course ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(course.name, style = MaterialTheme.typography.titleMedium)
                                if (course.meetingTime.isNotBlank()) {
                                    Text(
                                        course.meetingTime,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (course.location.isNotBlank()) {
                                    Text(
                                        course.location,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            TextButton(
                                onClick = onDirectionsClick,
                                modifier = Modifier.semantics {
                                    contentDescription = "Open directions for ${course.name}"
                                }
                            ) { Text("Directions") }
                            IconButton(
                                onClick = { pendingRemoval = index },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFB3261E), CircleShape)
                                    .semantics { contentDescription = "Remove ${course.name}" }
                            ) { Text("×", color = Color.White, fontSize = 24.sp) }
                        }
                    }
                }
            }
        }
    }

    val removalIndex = pendingRemoval
    val classToRemove = removalIndex?.let { classes.getOrNull(it) }
    if (classToRemove != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove class?") },
            text = { Text("Are you sure you want to remove ${classToRemove.name} from your classes?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClassesChange(classes.filterIndexed { index, _ -> index != removalIndex })
                        pendingRemoval = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E))
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddClass) {
        AlertDialog(
            onDismissRequest = { showAddClass = false },
            title = { Text("Add class") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Class name (required)") },
                        placeholder = { Text("Example: COMP 151") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = meetingTime,
                        onValueChange = { meetingTime = it },
                        label = { Text("Days and time (optional)") },
                        placeholder = { Text("Example: Mon/Wed 10:00 AM") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (optional)") },
                        placeholder = { Text("Example: Sierra Hall 101") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onClassesChange(
                            classes + MyPodClass(
                                name.trim(),
                                meetingTime.trim(),
                                location.trim()
                            )
                        )
                        name = ""
                        meetingTime = ""
                        location = ""
                        showAddClass = false
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddClass = false }) { Text("Cancel") }
            }
        )
    }
}
