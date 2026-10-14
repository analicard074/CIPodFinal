package com.example.cipod_final

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.cipod_final.navigationBar.NaviBar
import com.example.cipod_final.ui.login.LoginScreen

@Composable
@Preview
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }

    MaterialTheme {
        if (isLoggedIn) {
            NaviBar()
        } else {
            LoginScreen(onLoginSuccess = { isLoggedIn = true })
        }
    }
}
