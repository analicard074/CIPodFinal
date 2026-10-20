package com.example.cipod_final.buildings

/** Local directory. Evidence and coordinate review are recorded in docs/maps/README.md. */
object CampusBuildings {
    val all: List<CampusBuilding> = listOf(
        CampusBuilding(
            id = "gateway-hall",
            name = "Gateway Hall",
            description = "Welcome Center, classrooms, study spaces, and technology labs.",
            coordinates = BuildingCoordinates(latitude = 34.164829, longitude =-119.045050),
            universitySource = "https://maps.csuci.edu/?id=502#!m/803263",
            locationNote = "https://maps.csuci.edu/?id=502#!m/133412"
        ),
        CampusBuilding(
            id = "sierra-hall",
            name = "Sierra Hall",
            description = "Science and technology classrooms, teaching labs, and faculty offices.",
            coordinates = BuildingCoordinates(34.162197, -119.044662),
            universitySource = "https://maps.csuci.edu/?id=502#!m/133412",
            geographicSource = "https://www.openstreetmap.org/way/713268925"
        ),
        CampusBuilding(
            id = "broome-library",
            name = "Broome Library",
            description = "John Spoor Broome Library offers library collections, study areas, classrooms, and computer labs.",
            coordinates = BuildingCoordinates(34.162807, -119.041100),
            universitySource = "https://maps.csuci.edu/?id=502#!m/133389",
            geographicSource = "https://www.openstreetmap.org/way/461618288"
        )
    )

    fun find(id: String?): CampusBuilding? = all.find { it.id == id }

    fun search(query: String): List<CampusBuilding> {
        val term = query.trim()
        return all.filter { it.name.contains(term, ignoreCase = true) }
    }
}
