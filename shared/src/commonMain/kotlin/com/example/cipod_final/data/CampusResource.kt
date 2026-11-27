package com.example.cipod_final.data

enum class CampusResourceCategory(val label: String) {
    BASIC_NEEDS("Basic Needs"),
    FINANCIAL_AID("Financial Aid & Scholarships"),
    ACADEMIC_SUPPORT("Academic Support"),
    CULTURAL_IDENTITY("Cultural & Identity Centers"),
    HEALTH_WELLNESS("Health & Wellness"),
    STUDENT_SERVICES("Student Services"),
}

/** A campus service entry; navigation references an existing building ID, never coordinates. */
data class CampusResource(
    val id: String,
    val name: String,
    val category: CampusResourceCategory,
    val description: String,
    val officialUrl: String,
    val location: String? = null,
    val locationNote: String? = null,
    val contactInfo: String? = null,
    val appointmentInfo: String? = null,
    /** Set only when the existing campus map has a verified walking endpoint for this building. */
    val directionsBuildingId: String? = null,
    /** Existing room-directory ID used to open this resource in indoor directions, when supported. */
    val indoorDestinationRoomId: String? = null,
)
