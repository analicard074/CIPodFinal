package com.example.cipod_final.data.firestore

import com.example.cipod_final.data.auth.AuthRepository
import com.example.cipod_final.data.auth.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class FirebaseMyPodRepositoryTest {
    private val user = AuthUser("student-one", "student@myci.csuci.edu", false)
    private val selections = SavedMyPod(
        myPodClasses = listOf("__CIPOD_MYPOD_CLASSES_V2__", "COMP 105", "Intro", "Tue", "Gateway Hall", "Room 1"),
        myPodClubs = listOf("__CIPOD_MYPOD_CLUBS_V2__", "Data Science Club", "Description"),
    )

    @Test
    fun selectionsSurviveNewRepositoryAndAccountSwitchesIncludingRemoval() = runTest {
        val documents = mutableMapOf<String, SavedMyPod>()
        val auth = MyPodTestAuth(user)
        fun repository() = FirebaseMyPodRepository(
            auth,
            read = { documents[it] ?: SavedMyPod() },
            write = { uid, saved -> documents[uid] = saved },
        )
        assertEquals(SavedMyPod(), repository().load())
        repository().save(selections)
        assertEquals(selections, repository().load())
        auth.currentUser = user.copy(uid = "student-two")
        assertEquals(SavedMyPod(), repository().load())
        repository().save(SavedMyPod(myPodClubs = listOf("another club")))
        auth.currentUser = user
        assertEquals(selections, repository().load())
        repository().save(SavedMyPod())
        assertEquals(SavedMyPod(), repository().load())
    }

    @Test
    fun signedOutAndInvalidAccountsCannotReadOrWrite() = runTest {
        val auth = MyPodTestAuth(null)
        val repository = FirebaseMyPodRepository(
            auth,
            read = { error("Must not read storage") },
            write = { _, _ -> error("Must not write storage") },
        )
        assertFailsWith<IllegalStateException> { repository.load() }
        assertFailsWith<IllegalStateException> { repository.save(selections) }
        for (uid in listOf("", "../someone", ".", "..")) {
            auth.currentUser = user.copy(uid = uid)
            assertFailsWith<IllegalArgumentException> { repository.load() }
            assertFailsWith<IllegalArgumentException> { repository.save(selections) }
        }
    }

    @Test
    fun readFailureNeverWritesAnEmptySelection() = runTest {
        val failure = IllegalStateException("Permission denied")
        val repository = FirebaseMyPodRepository(
            MyPodTestAuth(user),
            read = { throw failure },
            write = { _, _ -> error("A failed read must not write anything") },
        )
        assertSame(failure, assertFailsWith<IllegalStateException> { repository.load() })
    }

    @Test
    fun writeFailuresAndCancellationArePropagated() = runTest {
        for (failure in listOf(IllegalStateException("Permission denied"), CancellationException())) {
            val repository = FirebaseMyPodRepository(
                MyPodTestAuth(user),
                read = { selections },
                write = { _, _ -> throw failure },
            )
            assertSame(failure, assertFailsWith<Exception> { repository.save(SavedMyPod()) })
            assertEquals(selections, repository.load())
        }
    }

    @Test
    fun accountChangeDuringReadDoesNotExposePreviousUsersSelections() = runTest {
        val auth = MyPodTestAuth(user)
        val repository = FirebaseMyPodRepository(
            auth,
            read = { auth.currentUser = user.copy(uid = "student-two"); selections },
            write = { _, _ -> },
        )
        assertFailsWith<IllegalStateException> { repository.load() }
    }
}

private class MyPodTestAuth(override var currentUser: AuthUser?) : AuthRepository {
    override val authState = MutableStateFlow(currentUser)
    override suspend fun signIn(email: String, password: String): AuthUser = error("Unused")
    override suspend fun createAccount(email: String, password: String): AuthUser = error("Unused")
    override suspend fun signOut() { currentUser = null }
}
