package com.example.cipod_final.data.firestore

data class UserProfile(val uid: String, val displayName: String?)

// Reads profiles and records successful app logins.
interface UserProfileRepository {
    // Reads users/{authenticated uid}; throws if no real Firebase user is signed in.
    suspend fun getCurrentUserProfile(): UserProfile?

    // Creates a missing profile, or updates only lastLoginAt for an existing one.
    suspend fun recordLogin()
}
