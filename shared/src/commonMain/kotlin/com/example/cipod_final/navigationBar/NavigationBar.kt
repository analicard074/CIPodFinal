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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipod_final.data.ClassSession
import com.example.cipod_final.buildings.CampusNavigationState
import com.example.cipod_final.ui.map.CampusMapScreen

private val CIBlue = Color(0xFF174A5D)

internal const val MY_POD_CLASS_STATE_V2_MARKER = "__CIPOD_MYPOD_CLASSES_V2__"
private const val MY_POD_CLASS_STATE_MARKER_PREFIX = "__CIPOD_MYPOD_CLASSES_V"

/** Encodes a versioned payload so future class model changes can be distinguished safely. */
internal fun encodeMyPodClassState(classes: List<MyPodClass>): List<String> =
    listOf(MY_POD_CLASS_STATE_V2_MARKER) + classes.flatMap { course ->
        listOf(course.code, course.name, course.meetingTime, course.building, course.floorAndRoom)
    }

/** Restores the current format or migrates complete records from the original three-field format. */
internal fun restoreMyPodClassState(values: List<String>): List<MyPodClass> {
    if (values.isEmpty()) return emptyList()

    val version = savedStateVersion(values.first())
    return when {
        version == "2" -> restoreCurrentClassRecords(values.drop(1))
        version != null -> emptyList() // Unknown versions are not interpreted as legacy records.
        else -> restoreLegacyClassRecords(values)
    }
}

private fun savedStateVersion(value: String): String? {
    if (!value.startsWith(MY_POD_CLASS_STATE_MARKER_PREFIX) || !value.endsWith("__")) return null
    val version = value.removePrefix(MY_POD_CLASS_STATE_MARKER_PREFIX).removeSuffix("__")
    return version.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
}

private fun restoreCurrentClassRecords(values: List<String>): List<MyPodClass> =
    values.chunked(5).mapNotNull { record ->
        if (record.size != 5) return@mapNotNull null
        MyPodClass(record[0], record[1], record[2], record[3], record[4])
    }

private fun restoreLegacyClassRecords(values: List<String>): List<MyPodClass> =
    values.chunked(3).mapNotNull { record ->
        if (record.size != 3) return@mapNotNull null

        // The old display name was always saved as "$courseCode: $courseName".
        val nameSeparator = record[0].indexOf(": ")
        if (nameSeparator <= 0) return@mapNotNull null

        val legacyLocation = record[2]
        val locationParts = LEGACY_LOCATION_PATTERN.matchEntire(legacyLocation)
        // Only split the exact format previously generated from the catalog. Unknown text is
        // intentionally omitted instead of guessing which parts are buildings, floors, or rooms.
        val building = locationParts?.groupValues?.get(1).orEmpty()
        val floorAndRoom = locationParts?.let {
            "Floor ${it.groupValues[2]}, Room ${it.groupValues[3]}"
        }.orEmpty()

        MyPodClass(
            code = record[0].substring(0, nameSeparator),
            name = record[0].substring(nameSeparator + 2),
            meetingTime = record[1],
            building = building,
            floorAndRoom = floorAndRoom,
        )
    }

private val LEGACY_LOCATION_PATTERN = Regex("^(.+), Floor (.+), Room (.+)$")

@Composable
fun NaviBar(
    modifier: Modifier = Modifier,
    onSignOut: suspend () -> Unit = {},
    loadClassSessions: suspend () -> List<ClassSession> = { emptyList() },
) {
    var selected by remember { mutableStateOf("MyPod") }
    // Keep expansion choices across tabs, and reset them for each login/app session.
    var classesExpanded by remember { mutableStateOf(true) }
    var clubsExpanded by remember { mutableStateOf(false) }
    var accessibilityEnabled by rememberSaveable { mutableStateOf(false) }
    var classes by rememberSaveable(stateSaver = listSaver<List<MyPodClass>, String>(
        save = { courses -> encodeMyPodClassState(courses) },
        restore = ::restoreMyPodClassState,
    )) { mutableStateOf(emptyList<MyPodClass>()) }

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
                    onNavigationChange = { navigation = it }
                )
                "MyPod" -> MyPodScreen(
                    classesExpanded = classesExpanded,
                    onClassesExpandedChange = { expanded ->
                        classesExpanded = expanded
                        if (expanded) clubsExpanded = false
                    },
                    clubsExpanded = clubsExpanded,
                    onClubsExpandedChange = { expanded ->
                        clubsExpanded = expanded
                        if (expanded) classesExpanded = false
                    },
                    loadClassSessions = loadClassSessions,
                    accessibilityEnabled = accessibilityEnabled,
                    onAccessibilityChange = { accessibilityEnabled = it },
                    classes = classes,
                    onClassesChange = { classes = it },
                    onDirectionsClick = { selected = "Directions" }
                )
                "Settings" -> SettingsScreen(
                    onSignOut = onSignOut,
                    accessibilityEnabled = accessibilityEnabled,
                    onAccessibilityChange = { accessibilityEnabled = it }
                )
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
