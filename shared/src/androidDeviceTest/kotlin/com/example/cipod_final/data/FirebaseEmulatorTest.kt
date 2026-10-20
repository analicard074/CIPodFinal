package com.example.cipod_final.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cipod_final.data.firestore.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Run only with the local Auth/Firestore emulators; no registered Firebase app is used. */
@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorTest {
    @Test
    fun signsInAndReadsOnlyItsOwnEmulatedProfile() = runBlocking<Unit> {
        withTimeout(45_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val app = Firebase.initialize(
                context,
                FirebaseOptions(
                    applicationId = "1:1234567890:android:0000000000000000",
                    apiKey = "AIzaSy" + "0".repeat(33), // Synthetic emulator-only identifier.
                    projectId = "demo-cipod-foundation",
                ),
                "emulator-test-${UUID.randomUUID()}",
            )
            val repositories = FirebaseRepositories.forLocalEmulators(app, "10.0.2.2")
            val nativeAuth = Firebase.auth(app)
            val firestore = Firebase.firestore(app)
            try {
                assertNull(repositories.auth.currentUser)
                assertFailsWith<IllegalStateException> { repositories.profiles.getCurrentUserProfile() }
                val email = "foundation-${UUID.randomUUID()}@example.invalid"
                // Transient test input, never saved in application state or configuration.
                val password = UUID.randomUUID().toString()
                nativeAuth.createUserWithEmailAndPassword(email, password)
                repositories.auth.signOut()
                assertFailsWith<FirebaseAuthException> { repositories.auth.signIn(email, "incorrect") }
                val user = repositories.auth.signIn(email, password)
                assertEquals(user.uid, repositories.auth.authState.first { it != null }?.uid)
                assertNull(repositories.profiles.getCurrentUserProfile())

                // This named app targets only demo-cipod-foundation on local emulators.
                val ownDocument = firestore.collection("users").document(user.uid)
                ownDocument.set(mapOf("displayName" to "Emulator User"))
                assertEquals(UserProfile(user.uid, "Emulator User"), repositories.profiles.getCurrentUserProfile())
                assertFailsWith<FirebaseFirestoreException> {
                    firestore.collection("users").document("another-user").get(Source.SERVER)
                }
                repositories.auth.signOut()
                assertNull(repositories.auth.authState.first())
                assertFailsWith<IllegalStateException> { repositories.profiles.getCurrentUserProfile() }
                assertFailsWith<FirebaseFirestoreException> { ownDocument.get(Source.SERVER) }
            } finally {
                nativeAuth.signOut()
                firestore.terminate()
                app.delete()
            }
        }
    }
}
