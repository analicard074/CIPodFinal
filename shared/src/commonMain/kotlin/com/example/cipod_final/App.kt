package com.example.cipod_final

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.example.cipod_final.data.FirebaseRepositories
import com.example.cipod_final.navigationBar.NaviBar
import com.example.cipod_final.ui.login.LoginScreen
import com.example.cipod_final.ui.theme.CIPodTheme

@Composable
@Preview
fun App(
    indoorEditorEnabled: Boolean = false,
    onLoginSuccess: () -> Unit = {},
) {
    val isPreview = LocalInspectionMode.current
    val firebase = remember(isPreview) { if (isPreview) null else FirebaseRepositories.create() }
    var isLoggedIn by remember { mutableStateOf(false) }

    CIPodTheme {
        if (isLoggedIn) {
            NaviBar(
                indoorEditorEnabled = indoorEditorEnabled,
                loadClassSessions = { checkNotNull(firebase).getClassSessions() },
                onSignOut = {
                    firebase?.auth?.signOut()
                    isLoggedIn = false
                },
            )
            LaunchedEffect(Unit) {
                onLoginSuccess()
            }
        } else {
            LoginScreen(onAuthenticate = { email, password, createAccount ->
                checkNotNull(firebase).authenticate(email, password, createAccount)
                isLoggedIn = true
            })
        }
    }
}
