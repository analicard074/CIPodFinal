package com.example.cipod_final

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.cipod_final.navigationBar.NaviBar
import com.example.cipod_final.ui.login.LoginScreen
import com.example.cipod_final.ui.theme.CIPodTheme

@Composable
@Preview
fun App(onLoginSuccess: () -> Unit = {}) {
    var isLoggedIn by remember { mutableStateOf(false) }

    CIPodTheme {
        if (isLoggedIn) {
            NaviBar()
            LaunchedEffect(Unit) {
                onLoginSuccess()
            }
        } else {
            LoginScreen(onLoginSuccess = { isLoggedIn = true })
        }
    }
}
