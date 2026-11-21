package com.example.cipod_final.indoor

object RoomDirectory {
    const val sourceUrl = "https://ciapps.csuci.edu/classroomsandlabs/"
    private val sierraRooms: List<DirectoryRoom> = listOf(
        "1111" to "Classroom", "1121" to "Classroom",
        "1131" to "Computer Lab", "1141" to "Computer Lab",
        "1212" to "Computer Lab", "1222" to "Computer Lab",
        "1232" to "Computer Lab", "1242" to "Computer Lab",
        "1314" to "Lab", "1324" to "Lab", "1334" to "Lab", "1344" to "Lab",
        "1411" to "Classroom", "1422" to "Classroom", "1432" to "Computer Lab"
    ).map { (number, type) ->
        DirectoryRoom("sierra-hall-$number", "sierra-hall", number, type, sourceUrl, "2026-10-18")
    }
    private const val broomeSourceURl = "https://library.csuci.edu/about/docs/jsbl-map-mar2023.pdf"

    private val broomeRooms: List<DirectoryRoom> = listOf(
        //First Floor
        "1310" to "Classroom",
        "1360" to "Classroom",
        "1710" to "Classroom",
        "1750" to "Classroom",
        "1756" to "Classroom",
        "1760" to "Classroom",
        //First Floor Services
        "1330" to "Computer Lab",
        "1531" to "Checkout",
        "1537" to "Cafe",
        "1550" to "Library Collections",
        "1720" to "Group Study",

        //Second Floor Classrooms
        "2325" to "Classroom",
        "2330" to "Classroom",
        "2480" to "Classroom",
        "2490" to "Classroom",

        //Second Floor Services + Study
        "2300" to "Quiet Study",
        "2360" to "Quiet Study",
        "2537" to "Gallery",
        "2675" to "Writing & Multiliteracy Center",
        "2680" to "Multiliteracy Room",
        "2720" to "Group Study",
        "2730" to "Learning Resource Center",
        "2740" to "Learning Resource Center",
    ).map { (number, type) ->
        val category = when (type) {
            "Classroom" -> RoomCategory.CLASSROOM
            "Computer Lab", "Checkout", "Cafe", "Gallery" ->
                RoomCategory.SERVICES
            "Group Study" -> RoomCategory.GROUP_STUDY
            "Library Collections" -> RoomCategory.LIBRARY_COLLECTION
            "Quiet Study" -> RoomCategory.QUIET_STUDY
            "Writing & Multiliteracy Center",
            "Multiliteracy Room",
            "Learning Resource Center" -> RoomCategory.CENTER

            else -> null
        }
        DirectoryRoom(
            id = "broome-library-$number",
            buildingId = "broome-library",
            number = number,
            publishedType = type,
            sourceUrl = broomeSourceURl,
            reviewedOn = "2026-11-19",
            category = category
        )
    }

    val rooms: List<DirectoryRoom> = sierraRooms + broomeRooms

    fun search(buildingId: String, query: String = ""): List<DirectoryRoom> = rooms.filter {
        it.buildingId == buildingId && (it.number.contains(query.trim(), ignoreCase = true) ||
            it.publishedType.contains(query.trim(), ignoreCase = true))
    }

    fun find(buildingId: String, roomId: String?): DirectoryRoom? =
        rooms.firstOrNull { it.buildingId == buildingId && it.id == roomId }
}
