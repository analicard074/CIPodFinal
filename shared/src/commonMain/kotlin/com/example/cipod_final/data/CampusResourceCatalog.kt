package com.example.cipod_final.data

/** Verified from official CSUCI pages; locations and schedules should be refreshed from those pages. */
object CampusResourceCatalog {
    val all: List<CampusResource> = listOf(
        CampusResource(
            id = "dolphin-pantry",
            name = "Dolphin Pantry",
            category = CampusResourceCategory.BASIC_NEEDS,
            description = "Free food, toiletries, and basic necessities for students in need.",
            officialUrl = "https://www.csuci.edu/student-life/basic-needs/food-assistance.html",
            location = "Arroyo Hall, Room 115",
            contactInfo = "805-437-2067 · basicneeds@csuci.edu",
            appointmentInfo = "Hours vary by term; check the official page. Friday visits are by appointment.",
        ),
        CampusResource(
            id = "financial-aid-scholarships",
            name = "Financial Aid & Scholarships",
            category = CampusResourceCategory.FINANCIAL_AID,
            description = "Help with grants, loans, scholarships, work-study, and financial aid eligibility.",
            officialUrl = "https://www.csuci.edu/cost-aid/financial-aid/",
            locationNote = "CSUCI pages list different office locations; confirm the current location with the office.",
            contactInfo = "805-437-8530 · financial.aid@csuci.edu",
        ),
        CampusResource(
            id = "learning-resource-center",
            name = "Learning Resource Center (LRC)",
            category = CampusResourceCategory.ACADEMIC_SUPPORT,
            description = "Peer tutoring, study skills support, and academic learning resources.",
            officialUrl = "https://www.csuci.edu/student-life/learning-resource-center/",
            location = "Broome Library, Room 2730",
            contactInfo = "805-437-8409 · lrc@csuci.edu",
            appointmentInfo = "Primarily drop-in; limited tutoring appointments are available.",
            directionsBuildingId = "broome-library",
            indoorDestinationRoomId = "broome-library-2730",
        ),
        CampusResource(
            id = "inclusive-student-services-center",
            name = "Inclusive Student Services Center",
            category = CampusResourceCategory.CULTURAL_IDENTITY,
            description = "Mentorship, intercultural programs, workshops, and support for an inclusive campus community.",
            officialUrl = "https://www.csuci.edu/student-life/cultural-centers/contact-us.html",
            location = "Bell Tower, Room 1506",
            contactInfo = "805-437-3528 · gary.gordon@csuci.edu",
            appointmentInfo = "Walk-ins and appointments are available; confirm current hours on the official page.",
        ),
        CampusResource(
            id = "black-cultural-center",
            name = "Black Cultural Center",
            category = CampusResourceCategory.CULTURAL_IDENTITY,
            description = "Community, programming, and support promoting Black student success and cultural awareness.",
            officialUrl = "https://www.csuci.edu/student-life/cultural-centers/contact-us.html",
            location = "Bell Tower, Room 1518",
            contactInfo = "805-437-3783",
        ),
        CampusResource(
            id = "latinx-cultural-center",
            name = "LatinX Cultural Center",
            category = CampusResourceCategory.CULTURAL_IDENTITY,
            description = "Programming, academic support, and community building for Latinx students and allies.",
            officialUrl = "https://www.csuci.edu/student-life/cultural-centers/latinx-cultural-center/index.html",
            location = "Bell Tower, Room 1538",
            contactInfo = "805-437-3773",
        ),
        CampusResource(
            id = "counseling-psychological-services",
            name = "Counseling & Psychological Services (CAPS)",
            category = CampusResourceCategory.HEALTH_WELLNESS,
            description = "Confidential short-term counseling, group services, psychiatric consultation, and crisis support.",
            officialUrl = "https://www.csuci.edu/office-departments/caps/faq.html",
            location = "Bell Tower East, Room 1867",
            contactInfo = "805-437-2088 · caps@csuci.edu",
            appointmentInfo = "Call or email for an appointment; urgent matters during business hours can be seen promptly.",
        ),
        CampusResource(
            id = "student-health-services",
            name = "Student Health Services",
            category = CampusResourceCategory.HEALTH_WELLNESS,
            description = "On-campus basic health care and preventive services for CSUCI students.",
            officialUrl = "https://www.csuci.edu/student-life/student-health/contact-us.html",
            location = "Yuba Hall",
            contactInfo = "805-437-8828 · Student.Health@csuci.edu",
            appointmentInfo = "Hours vary by academic term; see the official contact page.",
        ),
        CampusResource(
            id = "wellness-promotion-education",
            name = "Wellness Promotion & Education",
            category = CampusResourceCategory.HEALTH_WELLNESS,
            description = "Workshops, wellness campaigns, peer education, and holistic well-being programs.",
            officialUrl = "https://www.csuci.edu/student-life/studentsupport/syllabus.html",
            location = "Bell Tower, Room 1805",
            contactInfo = "805-437-3521 · wpe@csuci.edu",
        ),
        CampusResource(
            id = "career-development-alumni-engagement",
            name = "Career Development & Alumni Engagement",
            category = CampusResourceCategory.STUDENT_SERVICES,
            description = "Career exploration, job and internship search, resume support, and career counseling.",
            officialUrl = "https://www.csuci.edu/student-life/careerdevelopment/",
            location = "Bell Tower, Room 1548",
            contactInfo = "805-437-3270 · career.services@csuci.edu",
            appointmentInfo = "In-person and online appointments are offered; Friday appointments are virtual.",
        ),
        CampusResource(
            id = "student-business-services",
            name = "Student Business Services",
            category = CampusResourceCategory.STUDENT_SERVICES,
            description = "Support for tuition and fees, billing, payment plans, refunds, and student accounts.",
            officialUrl = "https://www.csuci.edu/office-departments/sbs/contact.html",
            location = "Marin Hall, first floor",
            contactInfo = "805-437-8810 · sbs@csuci.edu",
            appointmentInfo = "Office hours are published on the official contact page and may change for holidays.",
        ),
    )

    fun find(id: String?): CampusResource? = all.firstOrNull { it.id == id }

    fun search(query: String, category: CampusResourceCategory? = null): List<CampusResource> {
        val term = query.trim()
        return all.filter { resource ->
            (category == null || resource.category == category) &&
                (term.isEmpty() || resource.name.contains(term, ignoreCase = true) ||
                    resource.category.label.contains(term, ignoreCase = true) ||
                    resource.description.contains(term, ignoreCase = true))
        }
    }
}
