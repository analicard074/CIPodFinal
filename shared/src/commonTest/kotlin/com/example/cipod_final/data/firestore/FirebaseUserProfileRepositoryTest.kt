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

    private val user = AuthUser("test-user", "test@example.invalid", false)
}

private class FakeAuthRepository(override var currentUser: AuthUser?) : AuthRepository {
    override val authState = MutableStateFlow(currentUser)
    override suspend fun signIn(email: String, password: String): AuthUser = error("Unused in profile tests")
    override suspend fun signOut() {
        currentUser = null
    }
}
