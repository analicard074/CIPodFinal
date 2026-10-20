package com.example.cipod_final.data.auth

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?,
    val isEmailVerified: Boolean,
)

/** A real Firebase session. The demo login does not implement or update this state. */
interface AuthRepository {
    val currentUser: AuthUser?
    val authState: Flow<AuthUser?>

    /** Signs in an existing account; credentials are not retained by this repository. */
    suspend fun signIn(email: String, password: String): AuthUser
    suspend fun signOut()
}
