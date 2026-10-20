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

/** Explicit opt-in wiring; the app and demo login do not construct this container. */
class FirebaseRepositories private constructor(
    val auth: AuthRepository,
    val profiles: UserProfileRepository,
) {
    companion object {
        /** Uses the platform-configured default app. Does not sign in or read/write profiles. */
        fun create(): FirebaseRepositories {
            val auth = FirebaseAuthRepository()
            return FirebaseRepositories(auth, FirebaseUserProfileRepository(auth))
        }

        /** Requires a separate demo app; never redirects or reuses the production app/cache. */
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
