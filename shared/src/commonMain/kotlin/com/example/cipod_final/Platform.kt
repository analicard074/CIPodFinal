package com.example.cipod_final

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform