package com.example.cipod_final.navigationBar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.accessibility
import cipodfinal.shared.generated.resources.dolphin_squad
import com.example.cipod_final.buildings.CampusBuildings
import com.example.cipod_final.data.ClassSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
    onDirectionsClick: () -> Unit,
    loadClassSessions: suspend () -> List<ClassSession>,
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 25.dp)
                    .aspectRatio(1f),
            ) {
                Image(
                    painter = painterResource(Res.drawable.dolphin_squad),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter,
                )
            }

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
                    onDirectionsClick = onDirectionsClick,
                    loadClassSessions = loadClassSessions,
                )
            }
        }
    }
}

@Composable
private fun ClassesSection(
    classes: List<MyPodClass>,
    onClassesChange: (List<MyPodClass>) -> Unit,
    onDirectionsClick: () -> Unit,
    loadClassSessions: suspend () -> List<ClassSession>,
) {
    var showAddClass by rememberSaveable { mutableStateOf(false) }
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
        AddClassDialog(
            classes = classes,
            loadClassSessions = loadClassSessions,
            onDismiss = { showAddClass = false },
            onAdd = { course ->
                onClassesChange(classes + course)
                showAddClass = false
            },
        )
    }
}

@Composable
private fun AddClassDialog(
    classes: List<MyPodClass>,
    loadClassSessions: suspend () -> List<ClassSession>,
    onDismiss: () -> Unit,
    onAdd: (MyPodClass) -> Unit,
) {
    var sessions by remember { mutableStateOf(emptyList<ClassSession>()) }
    var search by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<ClassSession?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }

    // Read the catalog each time this dialog opens, or when the user chooses Retry.
    LaunchedEffect(retryCount) {
        loading = true
        loadFailed = false
        try {
            sessions = withTimeout(20_000) { loadClassSessions() }
        } catch (error: TimeoutCancellationException) {
            loadFailed = true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            loadFailed = true
        } finally {
            loading = false
        }
    }

    val query = search.trim()
    val matchingSessions = if (query.isEmpty()) {
        emptyList()
    } else {
        sessions.filter {
            it.courseCode.contains(query, ignoreCase = true) ||
                    it.courseName.contains(query, ignoreCase = true)
        }
    }
    val selectedClass = selectedSession?.toMyPodClass()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        selectedSession = null
                    },
                    label = { Text("Search classes") },
                    placeholder = { Text("Example: COMP 105") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !loading && !loadFailed,
                )
                when {
                    loading -> Text("Loading classes")
                    loadFailed -> {
                        Text(
                            "Unable to load classes. Please try again.",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { retryCount++ }) { Text("Retry") }
                    }

                    query.isEmpty() -> Text("Type a course code or name to find a class.")
                    sessions.isEmpty() -> Text("No classes are available yet.")
                    matchingSessions.isEmpty() -> Text("No matching classes found.")
                    else -> Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                            .selectableGroup(),
                    ) {
                        matchingSessions.forEach { session ->
                            val course = session.toMyPodClass()
                            val alreadyAdded = course in classes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = session == selectedSession,
                                        enabled = !alreadyAdded,
                                        role = Role.RadioButton,
                                        onClick = { selectedSession = session },
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = session == selectedSession,
                                    onClick = null,
                                    enabled = !alreadyAdded
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(course.name, style = MaterialTheme.typography.titleSmall)
                                    Text(course.meetingTime)
                                    Text(course.location)
                                    if (alreadyAdded) Text(
                                        "Already added",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && !loadFailed && selectedClass != null && selectedClass !in classes,
                onClick = { selectedClass?.let(onAdd) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// Convert the database's fields into the class card's existing display fields.
internal fun ClassSession.toMyPodClass(): MyPodClass {
    val buildingName = CampusBuildings.find(building)?.name ?: building.replace('-', ' ')
    val meetingDays = days.joinToString(" / ")
    return MyPodClass(
        name = "$courseCode: $courseName",
        meetingTime = "$meetingDays ${formatClassTime(startTime)}–${formatClassTime(endTime)}",
        location = "$buildingName, Floor $floor, Room $room",
    )
}

// Firestore stores minutes after midnight: 540 becomes 9:00 AM.
internal fun formatClassTime(minutesAfterMidnight: Int): String {
    require(minutesAfterMidnight in 0..1439) { "Time must be minutes after midnight." }
    val hour = minutesAfterMidnight / 60
    val minute = (minutesAfterMidnight % 60).toString().padStart(2, '0')
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val period = if (hour < 12) "AM" else "PM"
    return "$displayHour:$minute $period"
}
