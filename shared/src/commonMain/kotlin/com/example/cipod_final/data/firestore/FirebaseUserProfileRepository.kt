package com.example.cipod_final.data.firestore

import com.example.cipod_final.data.auth.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

/** Client checks complement Firestore Security Rules; they do not replace server authorization. */
class FirebaseUserProfileRepository internal constructor(
    private val authRepository: AuthRepository,
    private val readProfile: suspend (String) -> UserProfile?,
) : UserProfileRepository {
    constructor(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore = Firebase.firestore,
    ) : this(authRepository, { uid ->
        val snapshot = firestore.collection("users").document(uid).get()
        if (snapshot.exists) {
            UserProfile(uid, snapshot.data<ProfileDocument>().displayName)
        } else {
            null
        }
    })

    override suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = checkNotNull(authRepository.currentUser) {
            "A Firebase session is required to read a profile."
        }.uid
        require(uid.isNotBlank() && '/' !in uid && uid != "." && uid != "..") {
            "The user ID must be a single Firestore document ID."
        }
        val profile = readProfile(uid)
        check(authRepository.currentUser?.uid == uid) {
            "The Firebase session changed while reading the profile."
        }
        return profile
    }
}

@Serializable
private data class ProfileDocument(val displayName: String? = null)
