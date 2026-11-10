package com.example.cipod_final.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.dolphin_wave
import com.example.cipod_final.data.ProfileSaveException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoginScreen(
    onAuthenticate: suspend (email: String, password: String, createAccount: Boolean) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var creatingAccount by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ink = Color(0xFF0C4A54)

    fun isValidCIEmail(input: String): Boolean {
        val parts = input.trim().lowercase().split('@')
        return parts.size == 2 && parts[0].isNotBlank() &&
                parts[0].none { it.isWhitespace() } &&
                parts[1] in setOf("csuci.edu", "myci.csuci.edu")
    }

    fun submitLogin() {
        if (isLoading) return
        emailError = !isValidCIEmail(email)
        errorMessage = null
        if (emailError) return
        if (password.isBlank()) {
            errorMessage = "Please enter your password."
            return
        }
        if (creatingAccount && password.length < 6) {
            errorMessage = "Choose an app password with at least 6 characters."
            return
        }
        isLoading = true
        scope.launch {
            try {
                // Keep the existing Firebase login and registration behavior.
                withTimeout(30_000) {
                    onAuthenticate(email.trim().lowercase(), password, creatingAccount)
                }
                password = ""
                passwordVisible = false
            } catch (error: TimeoutCancellationException) {
                creatingAccount = false
                errorMessage = "The request timed out. Please try Log In again."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F8F9))
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.dolphin_wave),
            contentDescription = null,
            modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth().height(176.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(20.dp))

        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            contentColor = ink,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (creatingAccount) "Create an account" else "Welcome to CI Pod!",
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                if (creatingAccount) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Use your CSUCI email and create a password for CI Pod.",
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    errorContainerColor = Color(0xFFFAFAFA),
                    disabledContainerColor = Color(0xFFFAFAFA),
                    focusedBorderColor = ink,
                    unfocusedBorderColor = Color(0xFF71868D),
                    focusedTextColor = ink,
                    unfocusedTextColor = ink,
                    focusedPlaceholderColor = Color(0xFF607880),
                    unfocusedPlaceholderColor = Color(0xFF607880),
                )

                Text(
                    text = "CSUCI email",
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = false
                    },
                    enabled = !isLoading,
                    placeholder = { Text("Enter your CSUCI email") },
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "CSUCI email" },
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    singleLine = true,
                    isError = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                )
                if (emailError) {
                    Text(
                        "Please enter a valid CSUCI email address.",
                        modifier = Modifier.align(Alignment.Start),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Password",
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    enabled = !isLoading,
                    placeholder = { Text("Enter your password") },
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "Password" },
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                    trailingIcon = {
                        IconButton(
                            enabled = !isLoading,
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    if (passwordVisible) "Hide password" else "Show password"
                            },
                        ) { PasswordEye(passwordVisible) }
                    },
                )
                Spacer(Modifier.height(16.dp))
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { submitLogin() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10545F),
                        contentColor = Color.White
                    ),
                ) {
                    Text(
                        text = if (isLoading) "Please wait" else if (creatingAccount) "Create Account" else "Log In",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        creatingAccount = !creatingAccount
                        errorMessage = null
                        emailError = false
                        passwordVisible = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ink),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(if (creatingAccount) "Already have an account? " else "New to CI Pod? ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(if (creatingAccount) "Log In" else "Create an account")
                            }
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Find your way. Make CI yours.",
            color = ink,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// A small vector eye keeps the toggle sharp without adding another image file.
@Composable
private fun PasswordEye(passwordVisible: Boolean) {
    val outline = Color(0xFF607880)
    Canvas(Modifier.size(24.dp)) {
        val eye = Path().apply {
            moveTo(0f, size.height / 2)
            quadraticTo(size.width / 2, 0f, size.width, size.height / 2)
            quadraticTo(size.width / 2, size.height, 0f, size.height / 2)
            close()
        }
        val stroke = Stroke(width = 2.dp.toPx())
        drawPath(eye, outline, style = stroke)
        drawCircle(
            outline,
            radius = size.width * 0.14f,
            center = Offset(size.width / 2, size.height / 2),
            style = stroke
        )
        if (passwordVisible) {
            drawLine(
                outline,
                Offset(2.dp.toPx(), 2.dp.toPx()),
                Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
