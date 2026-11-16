package com.example.cipod_final.navigationBar

import com.example.cipod_final.data.Club
import kotlin.test.Test
import kotlin.test.assertEquals

class MyPodClubTest {
    @Test
    fun existingFirebaseFieldsBecomeNameAndDescription() {
        val club = Club(
            clubID = "Data Science Club",
            clubDescription = "A collaborative community for Data Science.",
        ).toMyPodClub()
        assertEquals(MyPodClub("Data Science Club", "A collaborative community for Data Science."), club)
    }

    @Test
    fun savedClubsRoundTripAndRejectUnknownFormats() {
        val clubs = listOf(
            MyPodClub("Data Science Club", "A collaborative community.\nAll students welcome."),
            MyPodClub("Art", ""),
        )
        assertEquals(clubs, restoreMyPodClubState(encodeMyPodClubState(clubs)))
        assertEquals(emptyList(), restoreMyPodClubState(emptyList()))
        assertEquals(emptyList(), restoreMyPodClubState(listOf("__CIPOD_MYPOD_CLUBS_V99__", "Art", "Description")))
        assertEquals(clubs, restoreMyPodClubState(encodeMyPodClubState(clubs) + listOf("incomplete")))
    }
}
