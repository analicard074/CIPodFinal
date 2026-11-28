package com.example.cipod_final.buildings

/** Local directory. Evidence and coordinate review are recorded in docs/maps/README.md. */
object CampusBuildings {
    val all: List<CampusBuilding> = listOf(
        CampusBuilding(
            id = "gateway-hall",
            name = "Gateway Hall",
            description = "Welcome Center, classrooms, study spaces, and technology labs.",
            coordinates = BuildingCoordinates(34.164838, -119.045057),
            universitySource = "https://maps.csuci.edu/?id=502#!m/1067463",
            geographicSource = "https://www.openstreetmap.org/#map=19/34.164626/-119.045022&layers=N"
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
            // Center of the OSM building footprint; the walking entrance is stored separately.
            coordinates = BuildingCoordinates(34.1627449, -119.0408063),
            universitySource = "https://maps.csuci.edu/?id=502#!m/133389",
            geographicSource = "https://www.openstreetmap.org/way/461618288"
        ),
        CampusBuilding(
            id = "petit-salon",
            name = "Petit Salon",
            description = "Campus event and gathering space.",
            // CSUCI map marker only; this is not an entrance or verified walking endpoint.
            coordinates = BuildingCoordinates(34.163609, -119.043770),
            universitySource = "https://maps.csuci.edu/?id=502#!m/133409",
            locationNote = "Official CSUCI map marker; Google Maps-reported main-door approach is provisional. The nearest mapped footway is 15.18 m away; the connecting approach is not separately mapped. Accessibility is unverified."
        )
    )

    fun find(id: String?): CampusBuilding? = all.find { it.id == id }

    fun search(query: String): List<CampusBuilding> {
        val term = query.trim()
        return all.filter { it.name.contains(term, ignoreCase = true) }
    }
}
