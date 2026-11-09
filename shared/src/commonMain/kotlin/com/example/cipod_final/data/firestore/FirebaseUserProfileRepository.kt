package com.example.cipod_final.data.firestore

import com.example.cipod_final.data.auth.AuthRepository
import com.example.cipod_final.data.auth.AuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

// Client checks complement Firestore Security Rules; they do not replace server authorization.
class FirebaseUserProfileRepository internal constructor(
    private val authRepository: AuthRepository,
    // Tests can supply these operations without connecting to a real Firebase project.
    private val writeLogin: suspend (AuthUser) -> Unit = { error("No profile writer configured.") },
    private val readProfile: suspend (String) -> UserProfile?,
) : UserProfileRepository {
    constructor(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore = Firebase.firestore,
    ) : this(
        authRepository = authRepository,
        writeLogin = { user -> saveLogin(firestore, user) },
        readProfile = { uid ->
            val snapshot = firestore.collection("users").document(uid).get()
            if (snapshot.exists) {
                UserProfile(uid, snapshot.data<ProfileDocument>().displayName)
            } else {
                null
            }
        },
    )

    override suspend fun recordLogin() {
        val user = requireCurrentUser()
        writeLogin(user)
        check(authRepository.currentUser?.uid == user.uid) {
            "The Firebase session changed while saving the profile."
        }
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = requireCurrentUser().uid
        val profile = readProfile(uid)
        check(authRepository.currentUser?.uid == uid) {
            "The Firebase session changed while reading the profile."
        }
        return profile
    }

    private fun requireCurrentUser(): AuthUser {
        val user = checkNotNull(authRepository.currentUser) {
            "Please log in."
        }
        require(user.uid.isNotBlank() && '/' !in user.uid && user.uid != "." && user.uid != "..") {
            "The user ID must be a single Firestore document ID."
        }
        return user
    }
}

@Serializable
private data class ProfileDocument(val displayName: String? = null)

// Keep the Firebase write here with the existing profile code.
private suspend fun saveLogin(firestore: FirebaseFirestore, user: AuthUser) {
    val email = checkNotNull(user.email) { "Your account needs an email address." }
    val userDocument = firestore.collection("users").document(user.uid)

    // A transaction prevents simultaneous logins from overwriting a profile's createdAt.
    firestore.runTransaction {
        val existingProfile = get(userDocument)
        if (existingProfile.exists) {
            // Returning user: preserve their name, email, and original creation time.
            updateFields(userDocument) {
                "lastLoginAt" to FieldValue.serverTimestamp
            }
        } else {
            // Firebase supplies the time, so it doesn't depend on the phone's clock.
            val newProfile = NewProfileDocument(
                displayName = email.substringBefore('@').take(100),
                email = email,
                createdAt = FieldValue.serverTimestamp,
                lastLoginAt = FieldValue.serverTimestamp,
            )
            set(userDocument, newProfile)
        }
    }
}

// @Serializable lets Firebase convert this Kotlin object into document fields.
@Serializable
private data class NewProfileDocument(
    val displayName: String,
    val email: String,
    val createdAt: FieldValue,
    val lastLoginAt: FieldValue,
)
