package com.example.cipod_final.indoor

/** Published directory facts. Room numbers do not imply floors or positions. */
data class DirectoryRoom(
    val id: String,
    val buildingId: String,
    val number: String,
    val publishedType: String,
    val sourceUrl: String,
    val reviewedOn: String
)
