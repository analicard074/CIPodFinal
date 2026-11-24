package com.example.cipod_final.indoor

data class RoomHighlight(
        val roomId: String,
        val floorPlanId: String,
        val planRevision: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        init {
            require(left >= 0f && top >= 0f)
            require(right <= 1f && bottom <= 1f)
            require(left < right && top < bottom)
        }
    }
