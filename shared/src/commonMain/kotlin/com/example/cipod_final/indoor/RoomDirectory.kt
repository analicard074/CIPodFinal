package com.example.cipod_final.indoor

object RoomDirectory {
    const val sourceUrl = "https://ciapps.csuci.edu/classroomsandlabs/"
    val rooms: List<DirectoryRoom> = listOf(
        "1111" to "Classroom", "1121" to "Classroom",
        "1131" to "Computer Lab", "1141" to "Computer Lab",
        "1212" to "Computer Lab", "1222" to "Computer Lab",
        "1232" to "Computer Lab", "1242" to "Computer Lab",
        "1314" to "Lab", "1324" to "Lab", "1334" to "Lab", "1344" to "Lab",
        "1411" to "Classroom", "1422" to "Classroom", "1432" to "Computer Lab"
    ).map { (number, type) ->
        DirectoryRoom("sierra-hall-$number", "sierra-hall", number, type, sourceUrl, "2026-10-18")
    }

    fun search(buildingId: String, query: String = ""): List<DirectoryRoom> = rooms.filter {
        it.buildingId == buildingId && (it.number.contains(query.trim(), ignoreCase = true) ||
            it.publishedType.contains(query.trim(), ignoreCase = true))
    }

    fun find(buildingId: String, roomId: String?): DirectoryRoom? =
        rooms.firstOrNull { it.buildingId == buildingId && it.id == roomId }
}
