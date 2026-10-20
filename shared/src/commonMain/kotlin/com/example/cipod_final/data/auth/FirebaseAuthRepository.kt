package com.example.cipod_final.data.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Construct only after platform Firebase initialization. No sign-in occurs on construction. */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
) : AuthRepository {
    override val currentUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    override val authState: Flow<AuthUser?>
        get() = auth.authStateChanged.map { it?.toAuthUser() }

    override suspend fun signIn(email: String, password: String): AuthUser =
        checkNotNull(auth.signInWithEmailAndPassword(email, password).user) {
            "Firebase sign-in did not return a user."
        }.toAuthUser()

    override suspend fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    isEmailVerified = isEmailVerified,
)
