package com.example.cipod_final.data

import com.example.cipod_final.data.auth.AuthRepository
import com.example.cipod_final.data.auth.FirebaseAuthRepository
import com.example.cipod_final.data.firestore.FirebaseUserProfileRepository
import com.example.cipod_final.data.firestore.UserProfileRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.memoryCacheSettings
import kotlinx.coroutines.CancellationException

// Firebase services shared by Android and iOS.
class FirebaseRepositories private constructor(
    val auth: AuthRepository,
    val profiles: UserProfileRepository,
) {
    // Open the main screen only after both authentication and profile saving succeed.
    suspend fun authenticate(email: String, password: String, createAccount: Boolean) {
        if (createAccount) {
            auth.createAccount(email, password)
        } else {
            auth.signIn(email, password)
        }

        try {
            profiles.recordLogin()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // The Auth account may already exist. Retry through Log In, not registration.
            throw ProfileSaveException(error)
        }
    }

    companion object {
        // Uses the platform-configured default app. Does not sign in or read/write profiles.
        fun create(): FirebaseRepositories {
            val auth = FirebaseAuthRepository()
            return FirebaseRepositories(auth, FirebaseUserProfileRepository(auth))
        }

        // Requires a separate demo app; never redirects or reuses the production app/cache.
        fun forLocalEmulators(app: FirebaseApp, host: String): FirebaseRepositories {
            require(app.options.projectId?.startsWith("demo-") == true) {
                "Emulator testing requires a separate demo- Firebase app."
            }
            require(host in setOf("127.0.0.1", "localhost", "10.0.2.2")) {
                "Use a local emulator host."
            }
            val nativeAuth = Firebase.auth(app)
            val firestore = Firebase.firestore(app)
            firestore.settings = firestoreSettings {
                cacheSettings = memoryCacheSettings { }
            }
            nativeAuth.useEmulator(host, 9099)
            firestore.useEmulator(host, 8080)
            val auth = FirebaseAuthRepository(nativeAuth)
            return FirebaseRepositories(auth, FirebaseUserProfileRepository(auth, firestore))
        }
    }
}

class ProfileSaveException(cause: Exception) : Exception(
    "Your account is signed in, but the profile could not be saved. Please try Log In again.",
    cause,
)
