package com.example.cipod_final.data.firestore

data class UserProfile(val uid: String, val displayName: String?)

/** Read-only foundation API. It cannot create, update, or delete student records. */
interface UserProfileRepository {
    /** Reads users/{authenticated uid}; throws if no real Firebase user is signed in. */
    suspend fun getCurrentUserProfile(): UserProfile?
}
