package com.example.cipod_final.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cipod_final.data.ProfileSaveException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun LoginScreen(
    onAuthenticate: suspend (email: String, password: String, createAccount: Boolean) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf(false) }
    var creatingAccount by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun isValidCIEmail(input: String): Boolean {
        val parts = input.trim().lowercase().split('@')
        return parts.size == 2 && parts[0].isNotBlank() &&
                parts[0].none { it.isWhitespace() } &&
                parts[1] in setOf("csuci.edu", "myci.csuci.edu")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simple coastal accents drawn in Compose; no image assets are needed.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(16.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
            Box(
                Modifier.size(24.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            )
            Box(Modifier.size(16.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CI Pod",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your campus. Your next adventure.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome aboard!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (creatingAccount) {
                        "Use your CSUCI email and create a password for this app."
                    } else {
                        "Log in with your CSUCI email and CIPod password."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    errorContainerColor = MaterialTheme.colorScheme.surface
                )

                OutlinedTextField(
                    enabled = !isLoading,
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = false
                    },
                    label = { Text("CSUCI Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors,
                    singleLine = true,
                    isError = emailError,
                    supportingText = {
                        if (emailError) {
                            Text("Please enter a valid CSUCI email address.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    enabled = !isLoading,
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("App password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        emailError = !isValidCIEmail(email)
                        errorMessage = null
                        if (!emailError && !isLoading) {
                            if (password.isBlank()) {
                                errorMessage = "Please enter your password."
                            } else if (creatingAccount && password.length < 6) {
                                errorMessage = "Choose an app password with at least 6 characters."
                            } else {
                                isLoading = true
                                scope.launch {
                                    try {
                                        // Wait for Firebase before opening the main screen.
                                        withTimeout(30_000) {
                                            onAuthenticate(
                                                email.trim().lowercase(),
                                                password,
                                                creatingAccount
                                            )
                                        }
                                        password = ""
                                    } catch (error: TimeoutCancellationException) {
                                        creatingAccount = false
                                        errorMessage =
                                            "The request timed out. Please try Log In again."
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        errorMessage = when (error) {
                                            is ProfileSaveException -> {
                                                creatingAccount = false
                                                error.message
                                            }

                                            is FirebaseAuthUserCollisionException -> {
                                                creatingAccount = false
                                                "This email already exists. Please log in."
                                            }

                                            is FirebaseAuthWeakPasswordException -> "Choose a stronger password."
                                            is FirebaseAuthInvalidCredentialsException,
                                            is FirebaseAuthInvalidUserException -> "Check your email and password."

                                            else -> "Unable to connect. Check your connection and try again."
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isLoading) "Please wait…" else if (creatingAccount) "Create Account" else "Log In")
                }
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        creatingAccount = !creatingAccount
                        errorMessage = null
                        emailError = false
                    },
                ) {
                    Text(if (creatingAccount) "Back to Log In" else "Create an account")
                }
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Find your way. Make CI yours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
