package com.example.cipod_final.navigationBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cipod_final.data.Club
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

data class MyPodClub(
    val name: String,
    val description: String,
)

@Composable
internal fun ClubsSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    clubs: List<MyPodClub>,
    onClubsChange: (List<MyPodClub>) -> Unit,
    loadClubs: suspend () -> List<Club>,
) {
    var showAddClub by rememberSaveable { mutableStateOf(false) }
    var pendingRemoval by rememberSaveable { mutableStateOf<Int?>(null) }
    val clubListColor = ButtonDefaults.elevatedButtonColors().containerColor

    CollapsiblePodSection(
        title = "Clubs",
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { showAddClub = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add club")
            }
        }
        if (clubs.isEmpty()) {
            Text(
                "No clubs added yet.",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 180.dp)
                    .background(clubListColor, MaterialTheme.shapes.medium)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                clubs.forEachIndexed { index, club ->
                    key(club) {
                        SwipeableClubCard(
                            club = club,
                            containerColor = clubListColor,
                            onRemove = { pendingRemoval = index },
                        )
                    }
                }
            }
        }
    }

    val removalIndex = pendingRemoval
    val clubToRemove = removalIndex?.let { clubs.getOrNull(it) }
    if (clubToRemove != null) {
        RemovePodItemDialog(
            itemType = "club",
            name = clubToRemove.name,
            collectionName = "clubs",
            onConfirm = {
                onClubsChange(clubs.filterIndexed { index, _ -> index != removalIndex })
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    if (showAddClub) {
        AddClubDialog(
            clubs = clubs,
            loadClubs = loadClubs,
            onDismiss = { showAddClub = false },
            onAdd = { club ->
                onClubsChange(clubs + club)
                showAddClub = false
            },
        )
    }
}

@Composable
private fun SwipeableClubCard(
    club: MyPodClub,
    containerColor: Color,
    onRemove: () -> Unit,
) {
    SwipeablePodCard(
        name = club.name,
        containerColor = containerColor,
        onRemove = onRemove,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp, end = 8.dp),
            ) {
                ClubDetails(club)
            }
        }
    }
}

@Composable
private fun AddClubDialog(
    clubs: List<MyPodClub>,
    loadClubs: suspend () -> List<Club>,
    onDismiss: () -> Unit,
    onAdd: (MyPodClub) -> Unit,
) {
    var sessions by remember { mutableStateOf(emptyList<Club>()) }
    var search by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<Club?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }

    // Read the catalog each time this dialog opens, or when the user chooses Retry.
    LaunchedEffect(retryCount) {
        loading = true
        loadFailed = false
        try {
            sessions = withTimeout(20.seconds) { loadClubs() }
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
            it.clubID.contains(query, ignoreCase = true)
        }
    }
    val selectedClub = selectedSession?.toMyPodClub()
    val searchFieldColor = ButtonDefaults.elevatedButtonColors().containerColor

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add club") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        selectedSession = null
                    },
                    label = { Text("Search clubs") },
                    placeholder = { Text("Example: Data Science Club") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !loading && !loadFailed,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = searchFieldColor,
                        unfocusedContainerColor = searchFieldColor,
                        disabledContainerColor = searchFieldColor,
                        errorContainerColor = searchFieldColor,
                    ),
                )
                when {
                    loading -> Text("Loading clubs")
                    loadFailed -> {
                        Text(
                            "Unable to load clubs. Please try again.",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { retryCount++ }) { Text("Retry") }
                    }

                    query.isEmpty() -> Text("Type a club name to find a club.")
                    sessions.isEmpty() -> Text("No clubs are available yet.")
                    matchingSessions.isEmpty() -> Text("No matching clubs found.")
                    else -> Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                            .selectableGroup(),
                    ) {
                        matchingSessions.forEach { session ->
                            val club = session.toMyPodClub()
                            val alreadyAdded = club in clubs
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
                                    ClubDetails(club)
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
                enabled = !loading && !loadFailed && selectedClub != null && selectedClub !in clubs,
                onClick = { selectedClub?.let(onAdd) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ClubDetails(club: MyPodClub) {
    var showMoreInfo by rememberSaveable(club) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            club.name,
            modifier = Modifier.weight(1f).padding(end = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = { showMoreInfo = true },
            modifier = Modifier.semantics {
                contentDescription = "More info about ${club.name}"
            },
        ) {
            Text("More info", style = MaterialTheme.typography.labelSmall)
        }
    }

    if (showMoreInfo) {
        AlertDialog(
            onDismissRequest = { showMoreInfo = false },
            title = { Text(club.name) },
            text = {
                Text(
                    club.description.ifBlank { "No description available." },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showMoreInfo = false }) { Text("Close") }
            },
        )
    }
}

internal fun Club.toMyPodClub(): MyPodClub = MyPodClub(
    name = clubID,
    description = clubDescription,
)

private const val MY_POD_CLUB_STATE_V2_MARKER = "__CIPOD_MYPOD_CLUBS_V2__"

internal fun encodeMyPodClubState(clubs: List<MyPodClub>): List<String> =
    listOf(MY_POD_CLUB_STATE_V2_MARKER) + clubs.flatMap {
        listOf(it.name, it.description)
    }

internal fun restoreMyPodClubState(values: List<String>): List<MyPodClub> {
    if (values.firstOrNull() != MY_POD_CLUB_STATE_V2_MARKER) return emptyList()
    return values.drop(1).chunked(2).mapNotNull {
        if (it.size != 2) null else MyPodClub(it[0], it[1])
    }
}
