package com.example.cipod_final.data.firestore

import com.example.cipod_final.data.auth.AuthRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

// Reuse the versioned screen-state formats for durable, per-user selections.
@Serializable
data class SavedMyPod(
    val myPodClasses: List<String> = emptyList(),
    val myPodClubs: List<String> = emptyList(),
)

class FirebaseMyPodRepository internal constructor(
    private val auth: AuthRepository,
    private val read: suspend (String) -> SavedMyPod,
    private val write: suspend (String, SavedMyPod) -> Unit,
) {
    constructor(auth: AuthRepository, firestore: FirebaseFirestore) : this(
        auth = auth,
        read = { uid ->
            val profile = firestore.collection("users").document(uid).get()
            check(profile.exists) { "Your profile is missing. Please log in again." }
            profile.data<SavedMyPod>()
        },
        write = { uid, selections ->
            // Update only selections, preserving the profile and login timestamps.
            firestore.collection("users").document(uid).update(
                mapOf(
                    "myPodClasses" to selections.myPodClasses,
                    "myPodClubs" to selections.myPodClubs,
                )
            )
        },
    )

    suspend fun load(): SavedMyPod {
        val uid = currentUid()
        val saved = read(uid)
        check(auth.currentUser?.uid == uid) { "The account changed while loading MyPod." }
        return saved
    }

    suspend fun save(selections: SavedMyPod) {
        val uid = currentUid()
        write(uid, selections)
        check(auth.currentUser?.uid == uid) { "The account changed while saving MyPod." }
    }

    private fun currentUid(): String {
        val uid = checkNotNull(auth.currentUser) { "Please log in." }.uid
        require(uid.isNotBlank() && '/' !in uid && uid != "." && uid != "..") {
            "The user ID must be a single Firestore document ID."
        }
        return uid
    }
}
