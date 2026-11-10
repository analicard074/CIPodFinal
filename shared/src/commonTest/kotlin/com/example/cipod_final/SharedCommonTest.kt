package com.example.cipod_final

import com.example.cipod_final.data.ClassSession
import com.example.cipod_final.navigationBar.formatClassTime
import com.example.cipod_final.navigationBar.toMyPodClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharedCommonTest {
    @Test
    fun classCardUsesTheFirestoreSchemaAndBuildingName() {
        val session = ClassSession(
            building = "gateway-hall",
            courseCode = "COMP 105",
            courseName = "Introduction To Programming",
            days = listOf("Tue", "Thu"),
            startTime = 540,
            endTime = 615,
            floor = 3,
            room = "3570",
        )
        val course = session.toMyPodClass()
        assertEquals("COMP 105: Introduction To Programming", course.name)
        assertEquals("Tue / Thu 9:00 AM–10:15 AM", course.meetingTime)
        assertEquals("Wed 9:00 AM–10:15 AM", session.copy(days = listOf("Wed")).toMyPodClass().meetingTime)
        assertEquals("Gateway Hall, Floor 3, Room 3570", course.location)
    }

    @Test
    fun classTimesHandleMidnightNoonAndAfternoon() {
        assertEquals("12:00 AM", formatClassTime(0))
        assertEquals("12:00 PM", formatClassTime(720))
        assertEquals("1:05 PM", formatClassTime(785))
        assertEquals("11:59 PM", formatClassTime(1439))
        assertFailsWith<IllegalArgumentException> { formatClassTime(-1) }
        assertFailsWith<IllegalArgumentException> { formatClassTime(1440) }
    }
}
