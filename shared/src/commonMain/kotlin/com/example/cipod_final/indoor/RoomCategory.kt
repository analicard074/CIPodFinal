package com.example.cipod_final.indoor

import androidx.compose.ui.graphics.Color

enum class RoomCategory (
    val label: String,
    val color: Color
) {
    CLASSROOM("Classroom", Color(0xFFF4BD6B)),
    SERVICES("Services", Color(0xFFFFF078)),
    GROUP_STUDY("Group Study", Color(0xFF71BFA7)),
    LIBRARY_COLLECTION("Library Collections", Color(0xFF87C6DF)),
    CENTER("Centers", Color(0xFF9595C5)),
    QUIET_STUDY("Quiet Study", Color(0xFFA77BAE)),
    OFFICE("Offices", Color(0xFFB9B9B9)),
    EMERGENCY_EXIT("Emergency Exit", Color(0xFFD9695E))
}