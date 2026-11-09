package com.example.cipod_final.data.firestore

import com.example.cipod_final.data.auth.AuthRepository
import com.example.cipod_final.data.auth.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class FirebaseUserProfileRepositoryTest {
    @Test
    fun demoOrSignedOutSessionNeverReadsFirestore() = runTest {
        var read = false
        val repository = FirebaseUserProfileRepository(FakeAuthRepository(null)) {
            read = true
            null
        }
        assertFailsWith<IllegalStateException> { repository.getCurrentUserProfile() }
        assertFalse(read)
    }

    @Test
    fun readsOnlyTheAuthenticatedUsersProfile() = runTest {
        val expected = UserProfile("test-user", "Emulator User")
        val repository = FirebaseUserProfileRepository(FakeAuthRepository(user)) { uid ->
            assertEquals(user.uid, uid)
            expected
        }
        assertEquals(expected, repository.getCurrentUserProfile())
    }

    @Test
    fun missingProfileDoesNotCreateOne() = runTest {
        val repository = FirebaseUserProfileRepository(FakeAuthRepository(user)) { null }
        assertNull(repository.getCurrentUserProfile())
    }

    @Test
    fun rejectsDocumentPathInjectionBeforeReading() = runTest {
        var read = false
        val auth = FakeAuthRepository(user.copy(uid = "other/profile"))
        val repository = FirebaseUserProfileRepository(auth) {
            read = true
            null
        }
        assertFailsWith<IllegalArgumentException> { repository.getCurrentUserProfile() }
        assertFalse(read)
    }

    @Test
    fun discardsProfileIfSessionChangesDuringRead() = runTest {
        val auth = FakeAuthRepository(user)
        val repository = FirebaseUserProfileRepository(auth) { uid ->
            auth.currentUser = user.copy(uid = "another-user")
            UserProfile(uid, "Emulator User")
        }
        assertFailsWith<IllegalStateException> { repository.getCurrentUserProfile() }
    }

    @Test
    fun propagatesPermissionErrorsAndCancellation() = runTest {
        for (failure in listOf(IllegalStateException("Permission denied"), CancellationException())) {
            val repository = FirebaseUserProfileRepository(FakeAuthRepository(user)) { throw failure }
            val thrown = assertFailsWith<Exception> { repository.getCurrentUserProfile() }
            assertSame(failure, thrown)
        }
    }

    @Test
    fun signedOutSessionCannotWriteAProfile() = runTest {
        var wroteProfile = false
        val repository = FirebaseUserProfileRepository(
            authRepository = FakeAuthRepository(null),
            writeLogin = { wroteProfile = true },
            readProfile = { null },
        )
        assertFailsWith<IllegalStateException> { repository.recordLogin() }
        assertFalse(wroteProfile)
    }

    @Test
    fun profileWriteUsesOnlyTheAuthenticatedUser() = runTest {
        var savedUser: AuthUser? = null
        val repository = FirebaseUserProfileRepository(
            authRepository = FakeAuthRepository(user),
            writeLogin = { savedUser = it },
            readProfile = { null },
        )
        repository.recordLogin()
        assertEquals(user, savedUser)
    }

    @Test
    fun invalidUserIdCannotWriteAProfile() = runTest {
        var wroteProfile = false
        val repository = FirebaseUserProfileRepository(
            authRepository = FakeAuthRepository(user.copy(uid = "other/profile")),
            writeLogin = { wroteProfile = true },
            readProfile = { null },
        )
        assertFailsWith<IllegalArgumentException> { repository.recordLogin() }
        assertFalse(wroteProfile)
    }

    @Test
    fun profileWritesPropagateErrorsAndCancellation() = runTest {
        for (failure in listOf(IllegalStateException("Permission denied"), CancellationException())) {
            val repository = FirebaseUserProfileRepository(
                authRepository = FakeAuthRepository(user),
                writeLogin = { throw failure },
                readProfile = { null },
            )
            assertSame(failure, assertFailsWith<Exception> { repository.recordLogin() })
        }
    }

    @Test
    fun sessionChangeDuringWriteDoesNotCompleteLogin() = runTest {
        val auth = FakeAuthRepository(user)
        val repository = FirebaseUserProfileRepository(
            authRepository = auth,
            writeLogin = { auth.currentUser = null },
            readProfile = { null },
        )
        assertFailsWith<IllegalStateException> { repository.recordLogin() }
    }

    private val user = AuthUser("test-user", "test@example.invalid", false)
}

private class FakeAuthRepository(override var currentUser: AuthUser?) : AuthRepository {
    override val authState = MutableStateFlow(currentUser)
    override suspend fun signIn(email: String, password: String): AuthUser = error("Unused in profile tests")
    override suspend fun createAccount(email: String, password: String): AuthUser = error("Unused in profile tests")
    override suspend fun signOut() {
        currentUser = null
    }
}
