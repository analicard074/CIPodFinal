package com.example.cipod_final.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cipod_final.data.firestore.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Run only with the local Auth/Firestore emulators; no registered Firebase app is used.
@RunWith(AndroidJUnit4::class)
class FirebaseEmulatorTest {
    @Test
    fun createsAccountAndRecordsOnlyItsOwnProfile() = runBlocking<Unit> {
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
                val catalogDocumentId = seedLocalClassCatalog()
                assertNull(repositories.auth.currentUser)
                assertFailsWith<IllegalStateException> { repositories.getClassSessions() }
                assertFailsWith<IllegalStateException> { repositories.profiles.getCurrentUserProfile() }
                val email = "foundation-${UUID.randomUUID()}@myci.csuci.edu"
                // Transient test input, never saved in application state or configuration.
                val password = UUID.randomUUID().toString()
                repositories.authenticate(email, password, createAccount = true)
                val user = checkNotNull(repositories.auth.currentUser)
                val expectedSession = ClassSession(
                    building = "gateway-hall",
                    courseCode = "COMP 105",
                    courseName = "Introduction To Programming",
                    days = listOf("Tue", "Thu"),
                    endTime = 615,
                    floor = 3,
                    room = "3570",
                    startTime = 540,
                )
                assertTrue(expectedSession in repositories.getClassSessions())
                val catalogDocument = firestore.collection("classSessions").document(catalogDocumentId)
                assertFailsWith<FirebaseFirestoreException> { catalogDocument.set(expectedSession) }
                assertFailsWith<FirebaseFirestoreException> { catalogDocument.delete() }
                val ownDocument = firestore.collection("users").document(user.uid)
                val firstProfile = ownDocument.get(Source.SERVER)
                val createdAt = firstProfile.get<Timestamp>("createdAt")
                val firstLogin = firstProfile.get<Timestamp>("lastLoginAt")
                assertEquals(createdAt, firstLogin)
                assertEquals(email, firstProfile.get<String>("email"))
                assertEquals(
                    UserProfile(user.uid, email.substringBefore('@')),
                    repositories.profiles.getCurrentUserProfile()
                )

                repositories.auth.signOut()
                assertFailsWith<FirebaseAuthException> {
                    repositories.authenticate(email, "incorrect", createAccount = false)
                }
                repositories.authenticate(email, password, createAccount = false)
                assertEquals(user.uid, repositories.auth.authState.first { it != null }?.uid)
                val nextProfile = ownDocument.get(Source.SERVER)
                assertEquals(createdAt, nextProfile.get<Timestamp>("createdAt"))
                assertEquals(
                    firstProfile.get<String>("displayName"),
                    nextProfile.get<String>("displayName")
                )
                val nextLogin = nextProfile.get<Timestamp>("lastLoginAt")
                assertTrue(
                    nextLogin.seconds > firstLogin.seconds ||
                            (nextLogin.seconds == firstLogin.seconds && nextLogin.nanoseconds > firstLogin.nanoseconds)
                )

                assertFailsWith<FirebaseFirestoreException> {
                    ownDocument.update(mapOf("createdAt" to Timestamp.now()))
                }
                assertFailsWith<FirebaseFirestoreException> {
                    ownDocument.update(mapOf("role" to "admin"))
                }
                assertFailsWith<FirebaseFirestoreException> { ownDocument.delete() }
                assertFailsWith<FirebaseFirestoreException> {
                    firestore.collection("users").document("another-user").get(Source.SERVER)
                }
                assertFailsWith<FirebaseFirestoreException> {
                    firestore.collection("users").document("another-user")
                        .set(mapOf("displayName" to "Other"))
                }
                assertFailsWith<FirebaseFirestoreException> {
                    firestore.collection("users").get(Source.SERVER)
                }

                // A login also repairs a profile missing after a failed registration write.
                repositories.auth.signOut()
                val repairEmail = "repair-${UUID.randomUUID()}@csuci.edu"
                nativeAuth.createUserWithEmailAndPassword(repairEmail, password)
                assertNull(repositories.profiles.getCurrentUserProfile())
                repositories.auth.signOut()
                repositories.authenticate(repairEmail, password, createAccount = false)
                assertEquals(
                    repairEmail.substringBefore('@'),
                    repositories.profiles.getCurrentUserProfile()?.displayName
                )
                repositories.auth.signOut()
                assertNull(repositories.auth.authState.first())
                assertFailsWith<FirebaseFirestoreException> {
                    firestore.collection("classSessions").get(Source.SERVER)
                }
                assertFailsWith<IllegalStateException> { repositories.profiles.getCurrentUserProfile() }
                assertFailsWith<FirebaseFirestoreException> { ownDocument.get(Source.SERVER) }
            } finally {
                nativeAuth.signOut()
                firestore.terminate()
                app.delete()
            }
        }
    }

    private fun seedLocalClassCatalog(): String {
        val documentId = "catalog-${UUID.randomUUID()}"
        val connection = URL(
            "http://10.0.2.2:8080/v1/projects/demo-cipod-foundation/databases/(default)/documents/classSessions/$documentId"
        ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PATCH"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = true
            // Emulator-only admin token: seed data that student clients cannot modify.
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write("""{"fields":{
                    "building":{"stringValue":"gateway-hall"},
                    "courseCode":{"stringValue":"COMP 105"},
                    "courseName":{"stringValue":"Introduction To Programming"},
                    "days":{"arrayValue":{"values":[{"stringValue":"Tue"},{"stringValue":"Thu"}]}},
                    "endTime":{"integerValue":"615"},
                    "floor":{"integerValue":"3"},
                    "room":{"stringValue":"3570"},
                    "startTime":{"integerValue":"540"}
                }}""")
            }
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
        return documentId
    }
}
