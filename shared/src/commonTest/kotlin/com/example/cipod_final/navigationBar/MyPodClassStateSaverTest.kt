package com.example.cipod_final.navigationBar

import kotlin.test.Test
import kotlin.test.assertEquals

class MyPodClassStateSaverTest {
    @Test
    fun emptyStateRestoresAsEmpty() {
        assertEquals(emptyList(), restoreMyPodClassState(emptyList()))
        assertEquals(emptyList(), restoreMyPodClassState(encodeMyPodClassState(emptyList())))
    }

    @Test
    fun legacyRecordsMigrateWithoutInventingLocationDetails() {
        val restored = restoreMyPodClassState(
            listOf(
                "COMP 105: Introduction To Programming",
                "Tue / Thu 9:00 AM–10:15 AM",
                "Gateway Hall, Floor 3, Room 3570",
                "BIO 100: Biology",
                "Mon 1:00 PM–2:00 PM",
                "North Annex",
            )
        )

        assertEquals(
            listOf(
                MyPodClass(
                    code = "COMP 105",
                    name = "Introduction To Programming",
                    meetingTime = "Tue / Thu 9:00 AM–10:15 AM",
                    building = "Gateway Hall",
                    floorAndRoom = "Floor 3, Room 3570",
                ),
                MyPodClass(
                    code = "BIO 100",
                    name = "Biology",
                    meetingTime = "Mon 1:00 PM–2:00 PM",
                    building = "",
                    floorAndRoom = "",
                ),
            ),
            restored,
        )
    }

    @Test
    fun legitimateLegacyNameThatResemblesAMarkerRemainsLegacyData() {
        val restored = restoreMyPodClassState(
            listOf(
                "__CIPOD_MYPOD_CLASSES_V3__: Example course",
                "Wed 10:00 AM–11:00 AM",
                "Unknown location",
            )
        )

        assertEquals("__CIPOD_MYPOD_CLASSES_V3__", restored.single().code)
        assertEquals("Example course", restored.single().name)
        assertEquals("", restored.single().building)
        assertEquals("", restored.single().floorAndRoom)
    }

    @Test
    fun currentFiveFieldRecordsRoundTrip() {
        val classes = listOf(
            MyPodClass("COMP 105", "Intro", "Tue 9:00 AM", "Gateway Hall", "Floor 3, Room 3570"),
            MyPodClass("BIO 100", "Biology", "Mon 1:00 PM", "Sierra Hall", "Floor 1, Room 1111"),
        )

        val encoded = encodeMyPodClassState(classes)

        assertEquals(MY_POD_CLASS_STATE_V2_MARKER, encoded.first())
        assertEquals(classes, restoreMyPodClassState(encoded))
    }

    @Test
    fun incompleteCurrentAndLegacyRecordsAreSkipped() {
        val current = encodeMyPodClassState(
            listOf(MyPodClass("COMP 105", "Intro", "Tue 9:00 AM", "Gateway Hall", "Floor 3, Room 3570"))
        ) + listOf("incomplete", "current", "record")
        val legacy = listOf(
            "COMP 105: Intro", "Tue 9:00 AM", "Gateway Hall, Floor 3, Room 3570",
            "incomplete", "legacy",
        )

        assertEquals(1, restoreMyPodClassState(current).size)
        assertEquals(1, restoreMyPodClassState(legacy).size)
        assertEquals(emptyList(), restoreMyPodClassState(listOf("COMP 105: Intro", "missing location")))
    }

    @Test
    fun unknownVersionsAndMalformedMarkersRestoreSafely() {
        assertEquals(
            emptyList(),
            restoreMyPodClassState(listOf("__CIPOD_MYPOD_CLASSES_V99__", "unknown", "payload")),
        )
        assertEquals(
            emptyList(),
            restoreMyPodClassState(listOf(MY_POD_CLASS_STATE_V2_MARKER, "only", "two")),
        )
        assertEquals(emptyList(), restoreMyPodClassState(listOf("__CIPOD_MYPOD_CLASSES_Vx__")))
    }
}
