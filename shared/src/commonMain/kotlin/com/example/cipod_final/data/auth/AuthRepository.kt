package com.example.cipod_final.data.auth

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?,
    val isEmailVerified: Boolean,
)

// A Firebase Authentication session.
interface AuthRepository {
    val currentUser: AuthUser?
    val authState: Flow<AuthUser?>

    // Signs in an existing account; credentials are not retained by this repository.
    suspend fun signIn(email: String, password: String): AuthUser

    // Creates an app account and signs in.
    suspend fun createAccount(email: String, password: String): AuthUser
    suspend fun signOut()
}
